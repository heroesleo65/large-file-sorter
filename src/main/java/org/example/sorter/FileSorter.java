package org.example.sorter;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.ProgressBar;
import org.example.sorter.chunks.FinalOutputChunk;
import org.example.sorter.chunks.OutputSortedChunk;
import org.example.sorter.chunks.TemporaryChunk;
import org.example.sorter.chunks.UnsortedChunk;

@Log4j2
public class FileSorter implements Closeable {

  private final Path input;
  private final Charset charset;
  private final ExecutorService executor;

  public FileSorter(Path input, Charset charset, int threadsCount) {
    if (threadsCount < 1) {
      throw new IllegalArgumentException("threadsCount must be greater than zero");
    }

    this.input = input;
    this.charset = charset;
    this.executor = Executors.newFixedThreadPool(threadsCount);
  }

  public void sort(
      int availableChunks, int chunkSize, int bufferSize, Path output
  ) throws InterruptedException {
    if (availableChunks < 3) {
      throw new IllegalArgumentException("availableChunks must be greater than three");
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

    try (var progressBar = new ProgressBar("Sorting", 0)) {
      int chunksCount = sortChunks(
          tempDirectory, readyChunks, workingChunks,
          availableChunks, chunkSize, bufferSize, progressBar
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
          chunksCount, availableChunks, chunkSize, outputFile, progressBar
      );
    }
  }

  @Override
  public void close() {
    executor.shutdown();

    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
          log.error("Executor in FileSorter did not terminate");
        }
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private int sortChunks(
      File tempDirectory, BlockingQueue<Integer> readyChunks, AtomicInteger workingChunks,
      int availableChunks, int chunkSize, int bufferSize, ProgressBar progressBar
  ) {
    int chunkNumber = 0;

    workingChunks.incrementAndGet();
    var chunk = new UnsortedChunk(
        FileHelper.getTemporaryFile(tempDirectory, chunkNumber++), chunkSize, bufferSize
    );

    var sortAndSaveAction = new SortAndSaveAction(workingChunks, progressBar);

    try (var bufferedReader = Files.newBufferedReader(input, charset)) {
      String line;

      while ((line = bufferedReader.readLine()) != null) {
        if (!chunk.add(line)) {
          if (workingChunks.incrementAndGet() < availableChunks) {
            executor.submit(new AsyncTask<>(sortAndSaveAction, chunk, readyChunks, chunkNumber));
          } else {
            sortAndSaveAction.accept(chunk);
            readyChunks.offer(chunkNumber);
          }

          chunk = new UnsortedChunk(
              FileHelper.getTemporaryFile(tempDirectory, chunkNumber++), chunkSize, bufferSize
          );
          chunk.add(line);
        }
      }
    } catch (FileNotFoundException ex) {
      System.err.println("Input file '" + input + "' not found");
      return 0;
    } catch (IOException ex) {
      log.error(() -> "Can't read data from file " + input, ex);
      System.err.println("Unknown exception in reading file " + input);
      return 0;
    }

    sortAndSaveAction.accept(chunk);
    readyChunks.offer(chunkNumber);

    return chunkNumber;
  }

  private void mergeChunks(
      File tempDirectory, BlockingQueue<Integer> readyChunks, AtomicInteger workingChunks,
      int chunksCount, int availableChunks, int chunkSize, File output, ProgressBar progressBar
  ) throws InterruptedException {

    var chunkNumber = chunksCount;
    progressBar.maxHint(progressBar.getMax() + chunksCount);

    do {
      var len = Integer.min(chunksCount, availableChunks - 1 - workingChunks.get());
      if (len >= 2 || (len == 1 && chunksCount == 1)) {
        var chunks = new TemporaryChunk[len];
        for (int i = 0; i < len; i++) {
          var file = FileHelper.getTemporaryFile(tempDirectory, readyChunks.take() - 1);
          chunks[i] = new TemporaryChunk(file, chunkSize);
        }

        chunksCount -= len - 1;

        Chunk chunk;
        if (chunksCount > 1) {
          chunk = new OutputSortedChunk(
              FileHelper.getTemporaryFile(tempDirectory, chunkNumber++), chunkSize
          );
        } else {
          chunk = new FinalOutputChunk(output, charset, chunkSize);
        }

        var merger = new ChunksMerger(chunk);
        merger.merge(chunks);
        readyChunks.offer(chunkNumber);
        progressBar.stepBy(len - 1);
      }
    } while (chunksCount > 1);

    progressBar.step();
  }

  @RequiredArgsConstructor
  private static class SortAndSaveAction implements Consumer<Chunk> {

    private final AtomicInteger counter;
    private final ProgressBar progressBar;

    @Override
    public void accept(Chunk chunk) {
      progressBar.maxHint(progressBar.getMax() + 1);

      chunk.sort();
      chunk.save();
      counter.decrementAndGet();

      progressBar.step();
    }
  }

  @RequiredArgsConstructor
  private static class AsyncTask<T> implements Runnable {

    private final Consumer<T> action;
    private final T object;
    private final BlockingQueue<Integer> queue;
    private final int value;

    @Override
    public void run() {
      action.accept(object);
      queue.offer(value);
    }
  }
}
