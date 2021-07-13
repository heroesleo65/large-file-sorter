package org.example.sorter.chunks;

import java.io.File;
import java.nio.charset.Charset;
import lombok.RequiredArgsConstructor;
import org.example.context.ApplicationContext;
import org.example.sorter.ChunkParameters;
import org.example.sorter.InputChunk;
import org.example.sorter.OutputChunk;

@RequiredArgsConstructor
public class ChunkFactory {
  private final File outputFile;
  private final Charset charset;
  private final ChunkParameters chunkParameters;
  private final ApplicationContext context;

  public InputChunk createInputSortedChunk(int chunkId) {
    return new InputSortedChunk(chunkId, chunkParameters.getChunkSize(), context);
  }

  public OutputChunk createOutputUnsortedChunk() {
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
}
