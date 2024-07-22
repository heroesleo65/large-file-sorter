package org.example.sorter.chunks;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Predicate;
import org.example.context.ApplicationContext;
import org.example.io.StringSerializer;
import org.example.sorter.CopyableOutputChunk;
import org.example.sorter.InputChunk;
import org.example.sorter.chunks.ids.OutputChunkId;

public abstract class AbstractCopyableOutputChunk extends AbstractOutputChunk
    implements CopyableOutputChunk {

  protected AbstractCopyableOutputChunk(
      OutputChunkId id, int chunkSize, StringSerializer serializer, ApplicationContext context
  ) {
    super(id, chunkSize, serializer, context);
  }

  @Override
  public boolean add(String line) {
    if (!super.add(line)) {
      save();
      return super.add(line);
    }
    return true;
  }

  @Override
  public String copyWithSaveUtil(InputChunk inputChunk, Predicate<String> predicate) {
    if (inputChunk instanceof AbstractInputChunk anotherChunk) {
      while (anotherChunk.nextLoad()) {
        if (!predicate.test(anotherChunk.data[anotherChunk.size - 1])) {
          int position = anotherChunk.cursor;
          while (predicate.test(anotherChunk.data[position])) {
            position++;
          }

          int count = position - anotherChunk.cursor;
          if (count != 0) {
            if (size + count < data.length) {
              System.arraycopy(anotherChunk.data, anotherChunk.cursor, data, size, count);
              size += count;
            } else {
              saveWithAdditionalData(anotherChunk.data, anotherChunk.cursor, position);
            }
          }
          anotherChunk.cursor = position + 1;
          return anotherChunk.data[position];
        }
        saveWithAdditionalData(anotherChunk.data, anotherChunk.cursor, anotherChunk.size);
        anotherChunk.cursor = anotherChunk.size;
      }
      anotherChunk.freeResources();
      return null;
    }

    var data = inputChunk.pop();
    while (data != null && predicate.test(data)) {
      add(data);
      data = inputChunk.pop();
    }
    return data;
  }

  @Override
  public void copyAndSave(InputChunk inputChunk) {
    if (inputChunk instanceof AbstractInputChunk anotherChunk) {
      save();

      while (anotherChunk.nextLoad()) {
        save(anotherChunk.data, anotherChunk.cursor, anotherChunk.size);
        anotherChunk.cursor = anotherChunk.size;
      }
      anotherChunk.freeResources();
    } else {
      var data = inputChunk.pop();
      while (data != null) {
        add(data);
        data = inputChunk.pop();
      }

      save();
    }
  }

  protected void saveWithAdditionalData(String[] data, int from, int to) {
    try (var stream = createOutputStream()) {
      saveWithAdditionalData(stream, data, from, to);
    } catch (IOException ex) {
      failSave(ex);
    }
  }

  protected void saveWithAdditionalData(OutputStream outputStream, String[] data, int from, int to)
      throws IOException {
    if (this.size != 0) {
      serializer.write(outputStream, this.data, 0, this.size);
      clear();
    }
    serializer.write(outputStream, data, from, to);
  }
}
