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
import org.example.progressbar.ProgressBar;
import org.example.progressbar.ProgressBarGroup;
import org.example.sorter.chunks.FinalOutputChunk;
import org.example.sorter.chunks.OutputSortedChunk;
import org.example.sorter.chunks.TemporaryChunk;
import org.example.sorter.chunks.UnsortedChunk;
import org.example.utils.ExecutorHelper;
import org.example.utils.FileHelper;
import org.jline.terminal.TerminalBuilder;

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

  public void sort(ChunkParameters chunkParameters, Path output) throws InterruptedException {
    if (chunkParameters.getAvailableChunks() < 3) {
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

    try (
        var terminal = TerminalBuilder.builder().dumb(true).build();
        var progressBarGroup = new ProgressBarGroup(terminal)
    ) {
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
    } catch (IOException ex) {
      log.error("This should never happen! Dumb terminal should have been created.", ex);
      System.err.println("This should never happen! Dumb terminal should have been created.");
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
        chunkParameters.getChunkSize(), chunkParameters.getBufferSize()
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
              chunkParameters.getChunkSize(), chunkParameters.getBufferSize()
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

    var chunkNumber = chunksCount;
    progressBar.maxHint(2L * chunksCount);

    do {
      var len = Integer.min(
          chunksCount, chunkParameters.getAvailableChunks() - 1 - workingChunks.get()
      );
      if (len >= 2 || (len == 1 && chunksCount == 1)) {
        var chunks = new TemporaryChunk[len];
        for (int i = 0; i < len; i++) {
          var file = FileHelper.getTemporaryFile(tempDirectory, readyChunks.take() - 1);
          chunks[i] = new TemporaryChunk(file, chunkParameters.getChunkSize());
        }

        chunksCount -= len - 1;

        Chunk chunk;
        if (chunksCount > 1) {
          chunk = new OutputSortedChunk(
              FileHelper.getTemporaryFile(tempDirectory, chunkNumber++),
              chunkParameters.getChunkSize(), chunkParameters.getBufferSize()
          );
        } else {
          chunk = new FinalOutputChunk(output, charset, chunkParameters.getChunkSize());
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
