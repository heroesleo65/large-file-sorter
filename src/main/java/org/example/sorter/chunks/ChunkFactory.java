package org.example.sorter.chunks;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Comparator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.context.ApplicationContext;
import org.example.sorter.InputChunk;
import org.example.sorter.OutputChunk;
import org.example.sorter.SortableOutputChunk;
import org.example.sorter.parameters.ChunkParameters;

@RequiredArgsConstructor
public class ChunkFactory {
  private final File outputFile;
  private final Charset charset;
  private final ChunkParameters chunkParameters;
  @Getter
  private final Comparator<String> comparator;
  private final ApplicationContext context;

  public InputChunk createInputSortedChunk(int chunkId) {
    return new InputSortedChunk(chunkId, chunkParameters.getChunkSize(), context);
  }

  public SortableOutputChunk createSortableOutputChunk() {
    return new OutputUnsortedChunk(
        context.getFileSystemContext().nextTemporaryFile(),
        chunkParameters.getChunkSize(),
        chunkParameters.getBufferSize(),
        comparator,
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
  }
}
