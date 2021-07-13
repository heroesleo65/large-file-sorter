package org.example.sorter.chunks;

import java.io.IOException;
import java.io.OutputStream;
import lombok.extern.log4j.Log4j2;
import org.example.context.ApplicationContext;
import org.example.utils.StreamHelper;

@Log4j2
public class OutputUnsortedChunk extends AbstractChunk {
  private static final int DEFAULT_BUFFER_SIZE = 128;

  private final int id;
  private final int bufferSize;
  private final ApplicationContext context;

  public OutputUnsortedChunk(int id, int chunkSize, ApplicationContext context) {
    this(id, chunkSize, DEFAULT_BUFFER_SIZE, context);
  }

  public OutputUnsortedChunk(int id, int chunkSize, int bufferSize, ApplicationContext context) {
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
  public boolean load() {
    throw new UnsupportedOperationException("Load is not supported");
  }

  @Override
  public void save() {
    char[] chars = null;
    byte[] bytes = null;
    try (var stream = context.getOutputStream(id)) {
      for (int i = 0; i < getCurrentSize(); i++) {
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
          StreamHelper.writeInt(stream, value.length);
          stream.write(value);
        }
      }
    } catch (IOException ex) {
      var file = context.getFileSystemContext().getTemporaryFile(id);
      log.error(() -> "Can't save file to temporary file '" + file + "'", ex);
      context.sendIOExceptionEvent(ex);
    }

    clear(/* dirty = */ false);
  }

  private void writeData(
      OutputStream stream, String line, char[] chars, byte[] bytes
  ) throws IOException {
    StreamHelper.writeInt(stream, 2 * line.length());

    int count;
    int offset = 0;
    while ((count = context.getStringContext().getValueArray(line, offset, chars, bytes)) > 0) {
      offset += count;
      stream.write(bytes, 0, 2 * count);
    }
  }
}
