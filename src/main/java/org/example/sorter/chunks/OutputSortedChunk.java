package org.example.sorter.chunks;

import java.io.IOException;
import org.example.context.ApplicationContext;
import org.example.io.StringSerializer;
import org.example.sorter.InputChunk;
import org.example.sorter.chunks.ids.OutputChunkId;

public class OutputSortedChunk extends AbstractCopyableOutputChunk {

  private final int bufferSize;

  public OutputSortedChunk(
      OutputChunkId id,
      int chunkSize,
      int bufferSize,
      StringSerializer serializer,
      ApplicationContext context
  ) {
    super(id, chunkSize, serializer, context);
    this.bufferSize = bufferSize;
  }

  @Override
  public void copyAndSave(InputChunk inputChunk) {
    if (inputChunk instanceof InputSortedChunk anotherChunk) {
      try (var stream = createOutputStream()) {
        saveWithAdditionalData(stream, anotherChunk.data, anotherChunk.cursor, anotherChunk.size);

        // Copy directly binary data from anotherChunk to this chunk
        anotherChunk.copyData(bufferSize, (bytes, len) -> stream.write(bytes, 0, len));
        anotherChunk.freeResources();
      } catch (IOException ex) {
        failSave(ex);
      }
    } else {
      super.copyAndSave(inputChunk);
    }
  }
}
