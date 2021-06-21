package org.example.sorter.chunks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.example.sorter.StringHelper;

public class UnsortedChunk extends AbstractChunk {
  private static final int COUNT_TEMP_DATA = 64;

  private final File outputFile;

  public UnsortedChunk(File outputFile, int chunkSize) {
    super(chunkSize);
    this.outputFile = outputFile;
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
            chars = new char[COUNT_TEMP_DATA];
          }
          if (bytes == null) {
            bytes = new byte[2 * COUNT_TEMP_DATA];
          }
          writeData(stream, line, chars, bytes);
        } else {
          writeInt(stream, value.length);
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
    writeInt(stream, 2 * line.length());

    int count;
    int offset = 0;
    while ((count = StringHelper.getValueArray(line, offset, chars, bytes)) > 0) {
      offset += count;
      stream.write(bytes, 0, 2 * count);
    }
  }

  private void writeInt(OutputStream stream, int value) throws IOException {
    stream.write((byte) ((value >>> 24) & 0xFF));
    stream.write((byte) ((value >>> 16) & 0xFF));
    stream.write((byte) ((value >>> 8) & 0xFF));
    stream.write((byte) ((value >>> 0) & 0xFF));
  }
}
