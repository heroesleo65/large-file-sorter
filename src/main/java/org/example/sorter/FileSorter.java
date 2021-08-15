package org.example.sorter;

import static org.example.sorter.SortState.MERGE;
import static org.example.sorter.SortState.PARTITION_SORT;
import static org.example.sorter.SortState.SAVE_OUTPUT;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.concurrent.BlockingBag;
import org.example.concurrent.BlockingSegments;
import org.example.concurrent.NonAsyncExecutorService;
import org.example.context.ApplicationContext;
import org.example.progressbar.ProgressBar;
import org.example.progressbar.ProgressBarGroup;
import org.example.sorter.chunks.ChunkFactory;
import org.example.sorter.parameters.ChunkParameters;
import org.example.utils.ExecutorHelper;

@Log4j2
public class FileSorter implements Closeable {

  private static final String PROGRESS_BAR_TASK_NAME = "Sorting...";

  private final Path input;
  private final Charset inputCharset;
  private final ExecutorService executor;
  private final ApplicationContext context;

  public FileSorter(Path input, Charset charset, int threadsCount, ApplicationContext context) {
    if (threadsCount < 1) {
      throw new IllegalArgumentException("threadsCount must be greater than zero");
    }

    this.input = input;
    this.inputCharset = charset;
    this.context = context;

    if (threadsCount == 1) {
      this.executor = new NonAsyncExecutorService();
    } else {
      var workQueue = new ArrayBlockingQueue<Runnable>(16);
      this.executor = new ThreadPoolExecutor(
          threadsCount - 1, threadsCount - 1,
          0L, TimeUnit.MILLISECONDS, workQueue, (task, executor) -> task.run()
      );
    }
  }

  public void sort(
      ChunkParameters chunkParameters,
      Comparator<String> comparator,
      Path output,
      Charset charset,
      boolean verbose
  ) throws InterruptedException {
    try {
      context.getFileSystemContext().createTemporaryDirectory();
    } catch (IOException ex) {
      System.err.println("Can't create temporary directory");
      return;
    }

    var workCounter = new AtomicInteger(0);
    BlockingBag chunksForProcessing = new BlockingSegments();

    try (var progressBarGroup = new ProgressBarGroup()) {
      var progressBar = progressBarGroup.createProgressBar(
          PROGRESS_BAR_TASK_NAME, /* initialMax = */ -1
      );

      var outputFile = output.toFile();
      var chunkFactory = new ChunkFactory(
          outputFile, charset, chunkParameters, comparator, context
      );

      if (!context.getFileSystemContext().delete(outputFile)) {
        System.err.println("Can't delete old file '" + output + "'");
        return;
      }

      long chunksCount = sortChunks(
          chunksForProcessing,
          workCounter,
          chunkParameters,
          chunkFactory,
          progressBar,
          verbose
      );
      if (chunksCount <= 1) {
        return;
      }

      mergeChunks(
          chunksForProcessing,
          workCounter,
          chunkParameters,
          chunksCount,
          chunkFactory,
          progressBar,
          verbose
      );
    }
  }

  @Override
  public void close() {
    if (!ExecutorHelper.close(executor)) {
      log.error("Executor in FileSorter did not terminate");
    }
  }

  private long sortChunks(
      BlockingBag chunksForProcessing,
      AtomicInteger workCounter,
      ChunkParameters chunkParameters,
      ChunkFactory chunkFactory,
      ProgressBar progressBar,
      boolean verbose
  ) {
    var allowableChunks = chunkParameters.getAllowableChunks(PARTITION_SORT);
    if (workCounter.incrementAndGet() > allowableChunks) {
      throw new IllegalArgumentException("allowableChunks is too small");
    }

    setState(PARTITION_SORT, /* maxHint = */ -1L, progressBar, verbose);

    var sortableOutputChunk = chunkFactory.createSortableOutputChunk(allowableChunks);
    long chunkNumber = 1L;

    var sortAndSaveAction = new SortAndSaveAction(workCounter, chunksForProcessing, progressBar);

    try (var bufferedReader = context.getStreamFactory().getBufferedReader(input, inputCharset)) {
      String line;

      while ((line = bufferedReader.readLine()) != null) {
        chunkParameters.addStringLength(line.length());

        if (!sortableOutputChunk.add(line)) {
          allowableChunks = chunkParameters.getAllowableChunks(PARTITION_SORT);
          if (workCounter.incrementAndGet() < allowableChunks) {
            final var chunk = sortableOutputChunk;
            executor.submit(() -> sortAndSaveAction.accept(chunk));
          } else {
            sortAndSaveAction.accept(sortableOutputChunk);
          }

          sortableOutputChunk = chunkFactory.createSortableOutputChunk(allowableChunks);
          chunkNumber++;

          if (!sortableOutputChunk.add(line)) {
            throw new IllegalArgumentException("Bad chunk was created");
          }
        }
      }
    } catch (NoSuchFileException ex) {
      System.err.println("Input file '" + input + "' not found");
      return 0;
    } catch (IOException ex) {
      log.error(() -> "Can't read data from file '" + input + "'", ex);
      System.err.println("Unknown exception in reading file '" + input + "'");
      return 0;
    }

    if (chunkNumber == 1) {
      setState(SAVE_OUTPUT, /* maxHint = */ 1L, progressBar, verbose);
      sortableOutputChunk.setId(chunkFactory.getFinalOutputChunkId());
      sortableOutputChunk.setStringSerializer(chunkFactory.getTextSerializer());
    }

    sortAndSaveAction.accept(sortableOutputChunk);

    return chunkNumber;
  }

  private void mergeChunks(
      BlockingBag chunksForProcessing,
      AtomicInteger workCounter,
      ChunkParameters chunkParameters,
      long remainingChunks,
      ChunkFactory chunkFactory,
      ProgressBar progressBar,
      boolean verbose
  ) throws InterruptedException {
    if (remainingChunks <= 0) {
      setState(SAVE_OUTPUT, /* maxHint = */ 0L, progressBar, verbose);
      return;
    }

    setState(MERGE, /* maxHint = */ 2L * remainingChunks, progressBar, verbose);

    int allowableChunks = chunkParameters.getAllowableChunks(MERGE);

    do {
      final int availableChunks = chunkParameters.getAvailableChunks(MERGE, remainingChunks);
      int curAvailableChunks;
      Runnable counterDecrementAction = () -> workCounter.addAndGet(-availableChunks);
      Consumer<Runnable> mergeAction;

      var currentWorkingChunks = workCounter.addAndGet(availableChunks);
      if (currentWorkingChunks < allowableChunks) {
        curAvailableChunks = availableChunks;
        mergeAction = executor::submit;
      } else {
        curAvailableChunks = allowableChunks - (currentWorkingChunks - availableChunks);
        if (curAvailableChunks > 2) {
          mergeAction = Runnable::run;
        } else {
          counterDecrementAction.run();
          continue;
        }
      }

      remainingChunks -= curAvailableChunks - 2;
      var chunks = getInputSortedChunks(curAvailableChunks - 1, chunkFactory, chunksForProcessing);
      if (remainingChunks > 1) {
        var action = new IntermediaMergeChunksAction(
            chunks,
            chunkFactory,
            chunksForProcessing,
            counterDecrementAction,
            progressBar
        );
        mergeAction.accept(action);
      } else {
        setState(SAVE_OUTPUT, progressBar, verbose);

        var action = new FinalMergeChunksAction(
            chunks,
            chunkFactory,
            counterDecrementAction,
            progressBar
        );

        action.run();
      }
    } while (remainingChunks > 1);

    progressBar.step();
  }

  private InputChunk[] getInputSortedChunks(
      int count, ChunkFactory chunkFactory, BlockingBag chunksForProcessing
  ) throws InterruptedException {
    var it = chunksForProcessing.takes(count).iterator();

    var chunks = new InputChunk[count];
    for (int i = 0; it.hasNext(); i++) {
      chunks[i] = chunkFactory.createInputSortedChunk(MERGE, count + 1, it.nextLong());
    }

    return chunks;
  }

  private void setState(SortState state, long maxHint, ProgressBar progressBar, boolean verbose) {
    progressBar.maxHint(maxHint);
    setState(state, progressBar, verbose);
  }

  private void setState(SortState state, ProgressBar progressBar, boolean verbose) {
    if (verbose) {
      progressBar.setExtraMessage(state.getDescription());
    }
  }

  private static abstract class MergeChunksAction {

    protected void merge(
        CopyableOutputChunk outputChunk,
        InputChunk[] chunks,
        Comparator<String> comparator,
        Runnable counterAction,
        ProgressBar progressBar
    ) {
      var merger = new ChunksMerger(outputChunk, comparator);
      merger.merge(chunks);

      counterAction.run();
      progressBar.stepBy(chunks.length - 1);
    }
  }

  @RequiredArgsConstructor
  private static class IntermediaMergeChunksAction extends MergeChunksAction implements Runnable {

    private final InputChunk[] chunks;
    private final ChunkFactory chunkFactory;
    private final BlockingBag bag;
    private final Runnable counterAction;
    private final ProgressBar progressBar;

    @Override
    public void run() {
      var outputChunk = chunkFactory.createTemporaryOutputSortedChunk(chunks.length + 1);

      merge(outputChunk, chunks, chunkFactory.getComparator(), counterAction, progressBar);

      bag.add(outputChunk.getId());
    }
  }

  @RequiredArgsConstructor
  private static class FinalMergeChunksAction extends MergeChunksAction implements Runnable {

    private final InputChunk[] chunks;
    private final ChunkFactory chunkFactory;
    private final Runnable counterAction;
    private final ProgressBar progressBar;

    @Override
    public void run() {
      var outputChunk = chunkFactory.createFinalOutputSortedChunk(chunks.length + 1);

      merge(outputChunk, chunks, chunkFactory.getComparator(), counterAction, progressBar);
    }
  }

  @RequiredArgsConstructor
  private static class SortAndSaveAction implements Consumer<OutputChunk> {

    private final AtomicInteger counter;
    private final BlockingBag bag;
    private final ProgressBar progressBar;

    @Override
    public void accept(OutputChunk chunk) {
      chunk.save();

      counter.decrementAndGet();
      bag.add(chunk.getId());

      progressBar.step();
    }
  }
}
