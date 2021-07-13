package org.example.sorter.chunks;

import java.io.File;
import java.nio.charset.Charset;
import lombok.RequiredArgsConstructor;
import org.example.context.ApplicationContext;
import org.example.sorter.Chunk;
import org.example.sorter.ChunkParameters;

@RequiredArgsConstructor
public class ChunkFactory {
  private final File outputFile;
  private final Charset charset;
  private final ChunkParameters chunkParameters;
  private final ApplicationContext context;

  public Chunk createInputSortedChunk(int chunkId) {
    return new InputSortedChunk(chunkId, chunkParameters.getChunkSize(), context);
  }

  public Chunk createOutputUnsortedChunk() {
    return new OutputUnsortedChunk(
        context.getFileSystemContext().nextTemporaryFile(),
        chunkParameters.getChunkSize(),
        chunkParameters.getBufferSize(),
        context
    );
  }

  public Chunk createTemporaryOutputSortedChunk() {
    return new OutputSortedChunk(
        context.getFileSystemContext().nextTemporaryFile(),
        chunkParameters.getChunkSize(),
        chunkParameters.getBufferSize(),
        context
    );
  }

  public Chunk createFinalOutputSortedChunk() {
    return new FinalOutputChunk(outputFile, charset, chunkParameters.getChunkSize(), context);
  }
}
