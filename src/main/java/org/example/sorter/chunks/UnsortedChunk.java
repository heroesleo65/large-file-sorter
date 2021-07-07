package org.example.sorter.chunks;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.example.io.FileOutputStreamFactory;
import org.example.io.OutputStreamFactory;
import org.example.utils.FileHelper;
import org.example.utils.StringHelper;

@Log4j2
public class UnsortedChunk extends AbstractChunk {
  private static final int DEFAULT_BUFFER_SIZE = 128;

  private final File outputFile;
  private final int bufferSize;

  @Setter
  private OutputStreamFactory outputStreamFactory;

  public UnsortedChunk(File outputFile, int chunkSize) {
    this(outputFile, chunkSize, DEFAULT_BUFFER_SIZE);
  }

  public UnsortedChunk(File outputFile, int chunkSize, int bufferSize) {
    super(chunkSize);
    this.outputFile = outputFile;
    this.bufferSize = bufferSize;
    this.outputStreamFactory = FileOutputStreamFactory.getInstance();
  }

  @Override
  public boolean load() {
    throw new UnsupportedOperationException("Load is not supported");
  }

  @Override
  public void save() {
    char[] chars = null;
    byte[] bytes = null;
    try (var stream = outputStreamFactory.getOutputStream(outputFile)) {
      for (int i = 0; i < getCurrentSize(); i++) {
        var line = data[i];

        stream.write(StringHelper.getCoder(line));

        var value = StringHelper.getValueArray(line);
        if (value == null) {
          if (chars == null) {
            chars = new char[bufferSize];
          }
          if (bytes == null) {
            bytes = new byte[2 * bufferSize];
          }
          writeData(stream, line, chars, bytes);
        } else {
          FileHelper.writeInt(stream, value.length);
          stream.write(value);
        }
      }
    } catch (IOException ex) {
      log.error(() -> "Can't save file '" + outputFile + "'", ex);
      // TODO: add processing error
    }

    clear(/* dirty = */ false);
  }

  private void writeData(
      OutputStream stream, String line, char[] chars, byte[] bytes
  ) throws IOException {
    FileHelper.writeInt(stream, 2 * line.length());

    int count;
    int offset = 0;
    while ((count = StringHelper.getValueArray(line, offset, chars, bytes)) > 0) {
      offset += count;
      stream.write(bytes, 0, 2 * count);
    }
  }
}
