package org.example.sorter.chunks;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;
import org.example.context.ApplicationContext;
import org.example.sorter.ChunkParameters;
import org.example.sorter.InputChunk;
import org.example.sorter.OutputChunk;

public class ChunkFactory {
  private final File outputFile;
  private final Charset charset;
  private final ChunkParameters chunkParameters;
  private final ApplicationContext context;
  private final AtomicReference<OutputUnsortedChunk> cacheOutputUnsortedChunk;

  public ChunkFactory(
      File outputFile, Charset charset, ChunkParameters chunkParameters, ApplicationContext context
  ) {
    this.outputFile = outputFile;
    this.charset = charset;
    this.chunkParameters = chunkParameters;
    this.context = context;
    this.cacheOutputUnsortedChunk = new AtomicReference<>();
  }

  public InputChunk createInputSortedChunk(int chunkId) {
    return new InputSortedChunk(chunkId, chunkParameters.getChunkSize(), context);
  }

  public OutputChunk createOutputUnsortedChunk() {
    var chunk = cacheOutputUnsortedChunk.getAndSet(null);
    if (chunk != null) {
      chunk.setId(context.getFileSystemContext().nextTemporaryFile());
      return chunk;
    }

    return new OutputUnsortedChunk(
        context.getFileSystemContext().nextTemporaryFile(),
        chunkParameters.getChunkSize(),
        chunkParameters.getBufferSize(),
        context
    );
  }

  public OutputChunk createTemporaryOutputSortedChunk() {
    return new OutputSortedChunk(
        context.getFileSystemContext().nextTemporaryFile(),
        chunkParameters.getChunkSize(),
        chunkParameters.getBufferSize(),
        context
    );
  }

  public OutputChunk createFinalOutputSortedChunk() {
    return new FinalOutputChunk(outputFile, charset, chunkParameters.getChunkSize(), context);
  }

  public void onFinishOutputChunkEvent(OutputChunk chunk) {
    if (chunk instanceof OutputUnsortedChunk) {
      cacheOutputUnsortedChunk.set((OutputUnsortedChunk) chunk);
    }
  }
}
