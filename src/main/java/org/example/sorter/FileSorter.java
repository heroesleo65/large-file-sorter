package org.example.sorter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
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
import org.example.utils.ExecutorHelper;

@Log4j2
public class FileSorter implements Closeable {

  private static final String PROGRESS_BAR_TASK_NAME = "Sorting...";
  private static final int MIN_CHUNKS_COUNT_FOR_MERGE = 3;

  private final Path input;
  private final Charset inputCharset;
  private final int threadsCount;
  private final ExecutorService executor;
  private final ApplicationContext context;

  public FileSorter(Path input, Charset charset, int threadsCount, ApplicationContext context) {
    if (threadsCount < 1) {
      throw new IllegalArgumentException("threadsCount must be greater than zero");
    }

    this.input = input;
    this.inputCharset = charset;
    this.threadsCount = threadsCount;
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

  public void sort(ChunkParameters chunkParameters, Path output, Charset charset)
      throws InterruptedException {
    if (chunkParameters.getAvailableChunks() < MIN_CHUNKS_COUNT_FOR_MERGE) {
      throw new IllegalArgumentException("availableChunks must be greater than or equal to three");
    }
    if (chunkParameters.getChunkSize() < 1) {
      throw new IllegalArgumentException("chunkSize must be positive value");
    }

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
      var chunkFactory = new ChunkFactory(outputFile, charset, chunkParameters, context);

      int chunksCount = sortChunks(
          chunksForProcessing,
          workCounter,
          chunkParameters,
          chunkFactory,
          progressBar
      );
      if (chunksCount <= 0) {
        return;
      }

      if (!context.getFileSystemContext().delete(outputFile)) {
        System.err.println("Can't delete old file '" + output + "'");
        return;
      }

      mergeChunks(
          chunksForProcessing,
          workCounter,
          chunkParameters.getAvailableChunks(),
          chunksCount,
          chunkFactory,
          progressBar
      );
    }
  }

  @Override
  public void close() {
    if (!ExecutorHelper.close(executor)) {
      log.error("Executor in FileSorter did not terminate");
    }
  }

  private int sortChunks(
      BlockingBag chunksForProcessing,
      AtomicInteger workCounter,
      ChunkParameters chunkParameters,
      ChunkFactory chunkFactory,
      ProgressBar progressBar
  ) {
    if (workCounter.incrementAndGet() > chunkParameters.getAvailableChunks()) {
      throw new IllegalArgumentException("allowableChunks is too small");
    }

    var sortableOutputChunk = chunkFactory.createSortableOutputChunk();
    int chunkNumber = 1;

    var sortAndSaveAction = new SortAndSaveAction(
        workCounter, chunksForProcessing, progressBar, chunkFactory
    );

    try (var bufferedReader = context.getStreamFactory().getBufferedReader(input, inputCharset)) {
      String line;

      long countLines = 0;
      long totalLength = 0;
      while ((line = bufferedReader.readLine()) != null) {
        totalLength += line.length();
        countLines++;

        chunkParameters.setAvgStringLength(totalLength, countLines);

        if (!sortableOutputChunk.add(line)) {
          if (workCounter.incrementAndGet() < chunkParameters.getAvailableChunks()) {
            final var chunk = sortableOutputChunk;
            executor.submit(() -> sortAndSaveAction.accept(chunk));
          } else {
            sortAndSaveAction.accept(sortableOutputChunk);
          }

          sortableOutputChunk = chunkFactory.createSortableOutputChunk();
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

    sortAndSaveAction.accept(sortableOutputChunk);

    return chunkNumber;
  }

  private void mergeChunks(
      BlockingBag chunksForProcessing,
      AtomicInteger workCounter,
      int allowableChunks,
      int remainingChunks,
      ChunkFactory chunkFactory,
      ProgressBar progressBar
  ) throws InterruptedException {
    if (remainingChunks <= 0) {
      return;
    }

    progressBar.maxHint(2L * remainingChunks);

    int allowableChunksPerThread = calculateAllowableChunksPerThread(allowableChunks, threadsCount);

    Runnable counterDecrementByCountChunksPerThreadAction =
        () -> workCounter.addAndGet(-allowableChunksPerThread);

    do {
      int currentWorkingChunks = workCounter.addAndGet(allowableChunksPerThread);
      if (currentWorkingChunks < allowableChunks) {
        int chunksForMerging = allowableChunksPerThread - 1;
        Runnable counterDecrementAction = counterDecrementByCountChunksPerThreadAction;

        if (allowableChunks > remainingChunks) {
          int diffChunks = remainingChunks - allowableChunksPerThread;
          if (diffChunks <= 0) {
            chunksForMerging = remainingChunks;
          } else if (workCounter.addAndGet(diffChunks) < allowableChunks) {
            chunksForMerging = remainingChunks;
            counterDecrementAction = () -> {
            };
          } else {
            workCounter.addAndGet(-diffChunks);
          }
        }
        var chunks = getInputSortedChunks(chunksForMerging, chunkFactory, chunksForProcessing);

        remainingChunks -= chunksForMerging - 1;

        if (remainingChunks > 1) {
          var action = new IntermediaMergeChunksAction(
              chunks,
              chunkFactory,
              chunksForProcessing,
              counterDecrementAction,
              progressBar
          );
          executor.submit(action);
        } else {
          var action = new FinalMergeChunksAction(
              chunks,
              chunkFactory,
              counterDecrementAction,
              progressBar
          );

          action.run();
        }
      } else {
        var availableChunks = allowableChunks - (currentWorkingChunks - allowableChunksPerThread);
        if (availableChunks > 1) {
          var chunksForMerging = Integer.min(remainingChunks, availableChunks - 1);

          var chunks = getInputSortedChunks(chunksForMerging, chunkFactory, chunksForProcessing);

          remainingChunks -= chunksForMerging - 1;

          Runnable action;
          if (remainingChunks > 1) {
            action = new IntermediaMergeChunksAction(
                chunks,
                chunkFactory,
                chunksForProcessing,
                counterDecrementByCountChunksPerThreadAction,
                progressBar
            );
          } else {
            action = new FinalMergeChunksAction(
                chunks,
                chunkFactory,
                counterDecrementByCountChunksPerThreadAction,
                progressBar
            );
          }

          action.run();
        } else {
          counterDecrementByCountChunksPerThreadAction.run();
        }
      }
    } while (remainingChunks > 1);

    progressBar.step();
  }

  private int calculateAllowableChunksPerThread(int allowableChunks, int threadsCount) {
    int result =
        Math.max(allowableChunks / threadsCount, MIN_CHUNKS_COUNT_FOR_MERGE) + (threadsCount >>> 1);
    return Math.min(result, allowableChunks);
  }

  private InputChunk[] getInputSortedChunks(
      int count, ChunkFactory chunkFactory, BlockingBag chunksForProcessing
  ) throws InterruptedException {
    var it = chunksForProcessing.takes(count).iterator();

    var chunks = new InputChunk[count];
    for (int i = 0; it.hasNext(); i++) {
      chunks[i] = chunkFactory.createInputSortedChunk(it.nextInt());
    }

    return chunks;
  }

  private static abstract class MergeChunksAction {

    protected void merge(
        OutputChunk outputChunk,
        InputChunk[] chunks,
        Runnable counterAction,
        ProgressBar progressBar
    ) {
      var merger = new ChunksMerger(outputChunk);
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
      var outputChunk = chunkFactory.createTemporaryOutputSortedChunk();

      merge(outputChunk, chunks, counterAction, progressBar);

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
      var outputChunk = chunkFactory.createFinalOutputSortedChunk();

      merge(outputChunk, chunks, counterAction, progressBar);
    }
  }

  @RequiredArgsConstructor
  private static class SortAndSaveAction implements Consumer<SortableOutputChunk> {

    private final AtomicInteger counter;
    private final BlockingBag bag;
    private final ProgressBar progressBar;
    private final ChunkFactory chunkFactory;

    @Override
    public void accept(SortableOutputChunk chunk) {
      chunk.sort();
      chunk.save();

      counter.decrementAndGet();
      bag.add(chunk.getId());

      chunkFactory.onFinishOutputChunkEvent(chunk);

      progressBar.step();
    }
  }
}
