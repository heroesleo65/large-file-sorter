package org.example.sorter.chunks;

import java.io.IOException;
import java.io.OutputStream;
import lombok.extern.log4j.Log4j2;
import org.example.context.ApplicationContext;
import org.example.sorter.InputChunk;
import org.example.utils.StreamHelper;

@Log4j2
public abstract class AbstractBinaryOutputChunk extends AbstractOutputChunk {

  private static final int DEFAULT_BUFFER_SIZE = 128;

  private int id;
  private final int bufferSize;
  private final ApplicationContext context;

  public AbstractBinaryOutputChunk(int id, int chunkSize, ApplicationContext context) {
    this(id, chunkSize, DEFAULT_BUFFER_SIZE, context);
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
  public int getId() {
    return id;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  protected void save(String[] data, int from, int to) {
    char[] chars = null;
    byte[] bytes = null;
    try (var stream = context.getOutputStream(id)) {
      for (int i = from; i < to; i++) {
        var line = data[i];

        stream.write(context.getStringContext().getCoder(line));

        var value = context.getStringContext().getValueArray(line);
        if (value == null) {
          if (chars == null) {
            chars = new char[bufferSize];
          }
          if (bytes == null) {
            bytes = new byte[2 * bufferSize];
          }
          writeData(stream, line, chars, bytes);
        } else {
          StreamHelper.writeVarint32(stream, value.length);
          stream.write(value);
        }
      }
    } catch (IOException ex) {
      var file = context.getFileSystemContext().getTemporaryFile(id);
      log.error(() -> "Can't save file to temporary file '" + file + "'", ex);
      context.sendSignal(ex);
    }
  }

  @Override
  public void copyAndSave(InputChunk inputChunk) {
    if (inputChunk instanceof InputSortedChunk) {
      save();

      var anotherChunk = (InputSortedChunk) inputChunk;
      if (anotherChunk.nextLoad()) {
        save(anotherChunk.data, anotherChunk.cursor, anotherChunk.size);
        anotherChunk.cursor = anotherChunk.size;

        // Copy directly binary data from anotherChunk to this chunk
        try (var stream = context.getOutputStream(id)) {
          final var monitoring = new CopyFileMonitoring();
          anotherChunk.loadData(bufferSize, (bytes, len) -> {
            try {
              stream.write(bytes, 0, len);
            } catch (IOException ex) {
              monitoring.exception = ex;
              return false;
            }
            return true;
          });
          monitoring.signal();
        } catch (IOException ex) {
          var file = context.getFileSystemContext().getTemporaryFile(id);
          log.error(() -> "Can't save file to temporary file '" + file + "'", ex);
          context.sendSignal(ex);
          return;
        }
      }
      anotherChunk.freeResources();
    } else {
      super.copyAndSave(inputChunk);
    }
  }

  private void writeData(
      OutputStream stream, String line, char[] chars, byte[] bytes
  ) throws IOException {
    StreamHelper.writeVarint32(stream, 2 * line.length());

    int count;
    int offset = 0;
    while ((count = context.getStringContext().getValueArray(line, offset, chars, bytes)) > 0) {
      offset += count;
      stream.write(bytes, 0, 2 * count);
    }
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
