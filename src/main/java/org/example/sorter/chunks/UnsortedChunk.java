package org.example.sorter.chunks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.example.sorter.utils.FileHelper;
import org.example.sorter.utils.StringHelper;

public class UnsortedChunk extends AbstractChunk {
  private static final int DEFAULT_BUFFER_SIZE = 128;

  private final File outputFile;
  private final int bufferSize;

  public UnsortedChunk(File outputFile, int chunkSize) {
    this(outputFile, chunkSize, DEFAULT_BUFFER_SIZE);
  }

  public UnsortedChunk(File outputFile, int chunkSize, int bufferSize) {
    super(chunkSize);
    this.outputFile = outputFile;
    this.bufferSize = bufferSize;
  }

  @Override
  public boolean load() {
    throw new UnsupportedOperationException("Load is not supported");
  }

  @Override
  public void save() {
    char[] chars = null;
    byte[] bytes = null;
    try (var stream = new FileOutputStream(outputFile, true)) {
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
      // ignore
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
