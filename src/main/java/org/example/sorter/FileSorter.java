package org.example.sorter;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ObjIntConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.context.ApplicationContext;
import org.example.progressbar.ProgressBar;
import org.example.progressbar.ProgressBarGroup;
import org.example.sorter.chunks.FinalOutputChunk;
import org.example.sorter.chunks.OutputSortedChunk;
import org.example.sorter.chunks.TemporaryChunk;
import org.example.sorter.chunks.UnsortedChunk;
import org.example.utils.ExecutorHelper;
import org.example.utils.FileHelper;

@Log4j2
public class FileSorter implements Closeable {

  private static final int MIN_CHUNKS_COUNT_FOR_MERGE = 3;

  private final Path input;
  private final Charset charset;
  private final ExecutorService executor;
  private final int threadsCount;
  private final ApplicationContext context;

  public FileSorter(Path input, Charset charset, int threadsCount, ApplicationContext context) {
    if (threadsCount < 1) {
      throw new IllegalArgumentException("threadsCount must be greater than zero");
    }

    this.input = input;
    this.charset = charset;
    this.threadsCount = threadsCount;
    this.executor = Executors.newFixedThreadPool(threadsCount);
    this.context = context;
  }

  public void sort(ChunkParameters chunkParameters, Path output) throws InterruptedException {
    if (chunkParameters.getAvailableChunks() < MIN_CHUNKS_COUNT_FOR_MERGE) {
      throw new IllegalArgumentException("availableChunks must be greater than or equal to three");
    }

    File tempDirectory;
    try {
      tempDirectory = Files.createTempDirectory(null).toFile();
    } catch (IOException ex) {
      System.err.println("Can't create temporary directory");
      return;
    }

    tempDirectory.deleteOnExit();

    var workingChunks = new AtomicInteger(0);
    BlockingQueue<Integer> readyChunks = new LinkedBlockingQueue<>();

    try (var progressBarGroup = new ProgressBarGroup()) {
      var progressBar = progressBarGroup.createProgressBar(
          /* task = */ "Sorting...", /* initialMax = */ -1
      );
      int chunksCount = sortChunks(
          tempDirectory, readyChunks, workingChunks, chunkParameters, progressBar
      );
      if (chunksCount <= 0) {
        return;
      }

      var outputFile = output.toFile();
      if (!FileHelper.safeDeleteFile(outputFile)) {
        System.err.println("Can't delete old file '" + output + "'");
        return;
      }

      mergeChunks(
          tempDirectory, readyChunks, workingChunks,
          chunksCount, chunkParameters, outputFile, progressBar
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
      File tempDirectory, BlockingQueue<Integer> readyChunks, AtomicInteger workingChunks,
      ChunkParameters chunkParameters, ProgressBar progressBar
  ) {
    int chunkNumber = 0;

    workingChunks.incrementAndGet();
    var chunk = new UnsortedChunk(
        FileHelper.getTemporaryFile(tempDirectory, chunkNumber++),
        chunkParameters.getChunkSize(), chunkParameters.getBufferSize(), context
    );

    var sortAndSaveAction = new SortAndSaveAction(workingChunks, progressBar, readyChunks);

    try (var bufferedReader = Files.newBufferedReader(input, charset)) {
      String line;

      while ((line = bufferedReader.readLine()) != null) {
        if (!chunk.add(line)) {
          if (workingChunks.incrementAndGet() < chunkParameters.getAvailableChunks()) {
            executor.submit(new AsyncTask<>(sortAndSaveAction, chunk, chunkNumber));
          } else {
            sortAndSaveAction.accept(chunk, chunkNumber);
          }

          chunk = new UnsortedChunk(
              FileHelper.getTemporaryFile(tempDirectory, chunkNumber++),
              chunkParameters.getChunkSize(), chunkParameters.getBufferSize(), context
          );
          chunk.add(line);
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

    sortAndSaveAction.accept(chunk, chunkNumber);

    return chunkNumber;
  }

  private void mergeChunks(
      File tempDirectory, BlockingQueue<Integer> readyChunks, AtomicInteger workingChunks,
      int chunksCount, ChunkParameters chunkParameters, File output, ProgressBar progressBar
  ) throws InterruptedException {

    var remainingChunks = chunksCount;
    var chunkNumber = chunksCount;
    progressBar.maxHint(2L * chunksCount);

    int allowableChunks = chunkParameters.getAvailableChunks();
    int allowableChunksPerThread = Math.min(
        Math.max(allowableChunks / threadsCount, MIN_CHUNKS_COUNT_FOR_MERGE) + threadsCount / 2,
        allowableChunks
    );

    Runnable counterDecrementByCountChunksByThreadAction =
        () -> workingChunks.addAndGet(-allowableChunksPerThread);

    do {
      int currentWorkingChunks = workingChunks.addAndGet(allowableChunksPerThread);
      if (currentWorkingChunks < allowableChunks) {
        int chunksForMerging = allowableChunksPerThread - 1;
        Runnable counterDecrementAction = counterDecrementByCountChunksByThreadAction;

        if (allowableChunks > remainingChunks) {
          int diffChunks = remainingChunks - allowableChunksPerThread;
          if (diffChunks <= 0) {
            chunksForMerging = remainingChunks;
          } else if (workingChunks.addAndGet(diffChunks) < allowableChunks) {
            chunksForMerging = remainingChunks;
            counterDecrementAction = () -> {
            };
          } else {
            workingChunks.addAndGet(-diffChunks);
          }
        }

        remainingChunks -= chunksForMerging - 1;

        var action = new MergeChunksAction(
            tempDirectory,
            readyChunks,
            getTemporaryChunks(tempDirectory, chunkParameters, readyChunks, chunksForMerging),
            remainingChunks,
            chunkNumber++,
            output,
            charset,
            chunkParameters,
            counterDecrementAction,
            context,
            progressBar
        );

        if (remainingChunks > 1) {
          executor.submit(action);
        } else {
          action.run();
        }
      } else {
        var availableChunks = allowableChunks - (currentWorkingChunks - allowableChunksPerThread);
        if (availableChunks > 1) {
          var chunksForMerging = Integer.min(remainingChunks, availableChunks - 1);
          remainingChunks -= chunksForMerging - 1;

          var action = new MergeChunksAction(
              tempDirectory,
              readyChunks,
              getTemporaryChunks(tempDirectory, chunkParameters, readyChunks, chunksForMerging),
              remainingChunks,
              chunkNumber++,
              output,
              charset,
              chunkParameters,
              counterDecrementByCountChunksByThreadAction,
              context,
              progressBar
          );

          action.run();
        } else {
          counterDecrementByCountChunksByThreadAction.run();
        }
      }
    } while (remainingChunks > 1);

    progressBar.step();
  }

  private TemporaryChunk[] getTemporaryChunks(
      File tempDirectory, ChunkParameters chunkParameters, BlockingQueue<Integer> queue, int count
  ) throws InterruptedException {
    var chunks = new TemporaryChunk[count];
    for (int i = 0; i < count; i++) {
      var file = FileHelper.getTemporaryFile(tempDirectory, queue.take() - 1);
      chunks[i] = new TemporaryChunk(file, chunkParameters.getChunkSize(), context);
    }

    return chunks;
  }

  @RequiredArgsConstructor
  private static class MergeChunksAction implements Runnable {

    private final File tempDirectory;
    private final BlockingQueue<Integer> queue;
    private final TemporaryChunk[] chunks;
    private final int remainingChunks;
    private final int chunkNumber;
    private final File output;
    private final Charset charset;
    private final ChunkParameters chunkParameters;
    private final Runnable counterAction;
    private final ApplicationContext context;
    private final ProgressBar progressBar;

    @Override
    public void run() {
      Chunk outputChunk;
      if (remainingChunks == 1) {
        outputChunk = new FinalOutputChunk(
            output, charset, chunkParameters.getChunkSize(), context
        );
      } else {
        outputChunk = new OutputSortedChunk(
            FileHelper.getTemporaryFile(tempDirectory, chunkNumber),
            chunkParameters.getChunkSize(), chunkParameters.getBufferSize(), context
        );
      }

      var merger = new ChunksMerger(outputChunk);
      merger.merge(chunks);

      counterAction.run();

      if (remainingChunks != 1) {
        queue.offer(chunkNumber + 1);
      }

      progressBar.stepBy(chunks.length - 1);
    }
  }

  @RequiredArgsConstructor
  private static class SortAndSaveAction implements ObjIntConsumer<Chunk> {

    private final AtomicInteger counter;
    private final ProgressBar progressBar;
    private final BlockingQueue<Integer> queue;

    @Override
    public void accept(Chunk chunk, int number) {
      chunk.sort();
      chunk.save();

      counter.decrementAndGet();
      queue.offer(number);

      progressBar.step();
    }
  }

  @RequiredArgsConstructor
  private static class AsyncTask<T> implements Runnable {

    private final ObjIntConsumer<T> action;
    private final T object;
    private final int value;

    @Override
    public void run() {
      action.accept(object, value);
    }
  }
}
