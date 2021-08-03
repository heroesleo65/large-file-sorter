package org.example.sorter.chunks;

import java.io.IOException;
import java.io.OutputStream;
import lombok.extern.log4j.Log4j2;
import org.example.context.ApplicationContext;
import org.example.sorter.InputChunk;
import org.example.sorter.parameters.DefaultParameters;
import org.example.utils.StreamHelper;

@Log4j2
public abstract class AbstractBinaryOutputChunk extends AbstractOutputChunk {

  private int id;
  private final int bufferSize;
  private final ApplicationContext context;

  public AbstractBinaryOutputChunk(int id, int chunkSize, ApplicationContext context) {
    this(id, chunkSize, DefaultParameters.DEFAULT_BUFFER_SIZE, context);
  }

  public AbstractBinaryOutputChunk(
      int id, int chunkSize, int bufferSize, ApplicationContext context
  ) {
    super(chunkSize);
    this.id = id;
    this.bufferSize = bufferSize;
    this.context = context;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  protected void save(String[] data, int from, int to) {
    try (var stream = context.getOutputStream(id)) {
      save(stream, data, from, to);
    } catch (IOException ex) {
      failSaveToFile(ex);
    }
  }

  @Override
  protected void saveWithAdditionalData(String[] data, int from, int to) {
    try (var stream = context.getOutputStream(id)) {
      if (size != 0) {
        save(stream, this.data, 0, size);
        clear();
      }
      save(stream, data, from, to);
    } catch (IOException ex) {
      failSaveToFile(ex);
    }
  }

  @Override
  public void copyAndSave(InputChunk inputChunk) {
    if (inputChunk instanceof InputSortedChunk) {
      try (var stream = context.getOutputStream(id)) {
        if (size != 0) {
          save(stream, data, 0, size);
          clear();
        }

        final var monitoring = new CopyFileMonitoring();

        // Copy directly binary data from anotherChunk to this chunk
        var anotherChunk = (InputSortedChunk) inputChunk;
        save(stream, anotherChunk.data, anotherChunk.cursor, anotherChunk.size);
        anotherChunk.loadData(bufferSize, (bytes, len) -> {
          try {
            stream.write(bytes, 0, len);
          } catch (IOException ex) {
            monitoring.exception = ex;
            return false;
          }
          return true;
        });
        anotherChunk.freeResources();

        monitoring.signal();
      } catch (IOException ex) {
        failSaveToFile(ex);
      }
    } else {
      super.copyAndSave(inputChunk);
    }
  }

  private void save(OutputStream stream, String[] data, int from, int to) throws IOException {
    // MetaData:
    // first byte - coder
    // 2-6 bytes - decoded len of string as 128 Base varint
    byte[] metaData = new byte[6];

    char[] chars = null;
    byte[] bytes = null;
    for (int i = from; i < to; i++) {
      var line = data[i];

      metaData[0] = context.getStringContext().getCoder(line);

      var value = context.getStringContext().getValueArray(line);
      if (value == null) {
        if (chars == null) {
          chars = new char[bufferSize >> 2];
        }
        if (bytes == null) {
          bytes = new byte[bufferSize >> 1];
        }
        int len = StreamHelper.writeVarint32(metaData, 1, 2 * line.length());
        stream.write(metaData, 0, len);
        writeData(stream, line, chars, bytes);
      } else {
        int len = StreamHelper.writeVarint32(metaData, 1, value.length);
        stream.write(metaData, 0, len);
        stream.write(value);
      }
    }
  }

  private void writeData(OutputStream stream, String line, char[] chars, byte[] bytes)
      throws IOException {
    int count;
    int offset = 0;
    while ((count = context.getStringContext().getValueArray(line, offset, chars, bytes)) > 0) {
      offset += count;
      stream.write(bytes, 0, 2 * count);
    }
  }

  private void failSaveToFile(IOException ex) {
    var file = context.getFileSystemContext().getTemporaryFile(id);
    log.error(() -> "Can't save file to temporary file '" + file + "'", ex);
    context.sendSignal(ex);
  }

  private static class CopyFileMonitoring {
    private IOException exception;

    public void signal() throws IOException {
      if (exception != null) {
        throw exception;
      }
    }
  }
}
