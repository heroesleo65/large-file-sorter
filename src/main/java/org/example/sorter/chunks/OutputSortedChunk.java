package org.example.sorter.chunks;

import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import org.example.context.ApplicationContext;
import org.example.io.StringSerializer;
import org.example.sorter.InputChunk;
import org.example.sorter.chunks.ids.OutputChunkId;

@Log4j2
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
    if (inputChunk instanceof InputSortedChunk) {
      try (var stream = createOutputStream()) {
        var anotherChunk = (InputSortedChunk) inputChunk;

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
