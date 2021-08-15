package org.example.sorter.chunks;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Comparator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.context.ApplicationContext;
import org.example.io.BinaryDeserializer;
import org.example.io.BinarySerializer;
import org.example.io.StringDeserializer;
import org.example.io.StringSerializer;
import org.example.io.TextSerializer;
import org.example.sorter.CopyableOutputChunk;
import org.example.sorter.InputChunk;
import org.example.sorter.OutputChunk;
import org.example.sorter.SortState;
import org.example.sorter.chunks.ids.FileOutputChunkId;
import org.example.sorter.chunks.ids.OutputChunkId;
import org.example.sorter.chunks.ids.TemporaryDataOutputChunkId;
import org.example.sorter.parameters.ChunkParameters;

@RequiredArgsConstructor
public class ChunkFactory {
  private final ChunkParameters chunkParameters;
  @Getter
  private final Comparator<String> comparator;
  private final ApplicationContext context;

  @Getter
  private final OutputChunkId finalOutputChunkId;

  private final StringSerializer binarySerializer;

  @Getter
  private final StringSerializer textSerializer;

  private final StringDeserializer binaryDeserializer;

  public ChunkFactory(
      File outputFile,
      Charset charset,
      ChunkParameters chunkParameters,
      Comparator<String> comparator,
      ApplicationContext context
  ) {
    this.chunkParameters = chunkParameters;
    this.comparator = comparator;
    this.context = context;

    this.finalOutputChunkId = new FileOutputChunkId(outputFile, context);

    this.binarySerializer = new BinarySerializer(
        chunkParameters.getBufferSize(), context.getStringContext()
    );
    this.textSerializer = new TextSerializer(charset);

    this.binaryDeserializer = new BinaryDeserializer(context);
  }

  public InputChunk createInputSortedChunk(SortState state, int chunks, long chunkId) {
    return new InputSortedChunk(
        chunkId,
        chunkParameters.getChunkSize(state, chunks),
        binaryDeserializer,
        context
    );
  }

  public OutputChunk createSortableOutputChunk(int chunks) {
    var chunkId = context.getFileSystemContext().nextTemporaryFile();
    return new SortableOutputChunk(
        new TemporaryDataOutputChunkId(chunkId, context),
        chunkParameters.getChunkSize(SortState.PARTITION_SORT, chunks),
        binarySerializer,
        comparator,
        context
    );
  }

  public CopyableOutputChunk createTemporaryOutputSortedChunk(int chunks) {
    var chunkId = context.getFileSystemContext().nextTemporaryFile();
    return new OutputSortedChunk(
        new TemporaryDataOutputChunkId(chunkId, context),
        chunkParameters.getChunkSize(SortState.MERGE, chunks),
        chunkParameters.getBufferSize(),
        binarySerializer,
        context
    );
  }

  public CopyableOutputChunk createFinalOutputSortedChunk(int chunks) {
    return new FinalOutputChunk(
        finalOutputChunkId,
        chunkParameters.getChunkSize(SortState.SAVE_OUTPUT, chunks),
        textSerializer,
        context
    );
  }
}
