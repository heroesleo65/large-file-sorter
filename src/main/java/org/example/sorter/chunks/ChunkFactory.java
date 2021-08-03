package org.example.sorter.chunks;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Comparator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.context.ApplicationContext;
import org.example.sorter.InputChunk;
import org.example.sorter.OutputChunk;
import org.example.sorter.SortState;
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

  public InputChunk createInputSortedChunk(SortState state, int chunks, long chunkId) {
    return new InputSortedChunk(chunkId, chunkParameters.getChunkSize(state, chunks), context);
  }

  public SortableOutputChunk createSortableOutputChunk(int chunks) {
    return new OutputUnsortedChunk(
        context.getFileSystemContext().nextTemporaryFile(),
        chunkParameters.getChunkSize(SortState.PARTITION_SORT, chunks),
        chunkParameters.getBufferSize(),
        comparator,
        context
    );
  }

  public OutputChunk createTemporaryOutputSortedChunk(int chunks) {
    return new OutputSortedChunk(
        context.getFileSystemContext().nextTemporaryFile(),
        chunkParameters.getChunkSize(SortState.MERGE, chunks),
        chunkParameters.getBufferSize(),
        context
    );
  }

  public OutputChunk createFinalOutputSortedChunk(int chunks) {
    return new FinalOutputChunk(
        outputFile, charset, chunkParameters.getChunkSize(SortState.SAVE_OUTPUT, chunks), context
    );
  }
}
