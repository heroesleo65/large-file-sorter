package org.example.sorter.chunks;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.example.sorter.StringHelper;

public class TemporaryChunk extends AbstractChunk {
  private final File inputFile;
  private long position;

  public TemporaryChunk(File inputFile, int chunkSize) {
    super(chunkSize);
    this.inputFile = inputFile;
  }

  @Override
  public String pop() {
    var result = super.pop();
    if (result == null && inputFile.exists() && inputFile.isFile()) {
      try {
        inputFile.delete();
      } catch (Exception ex) {
        // ignore
      }
    }
    return result;
  }

  @Override
  public boolean load() {
    if (!inputFile.exists() || !inputFile.isFile()) {
      return false;
    }

    try (var file = new RandomAccessFile(inputFile, "r")) {
      if (position >= file.length()) {
        return false;
      }
      file.seek(position);

      var values = new byte[0];
      var builder = new StringBuilder(0);
      for (int i = 0; i < data.length; i++) {
        int coder = file.read();
        if (coder < 0) {
          break;
        }

        var len = readInt(file);
        if (len < 0) {
          break;
        }

        if (StringHelper.hasSupportReflection() || values.length < len) {
          values = new byte[len];
        }
        file.read(values, 0, len);

        var line = StringHelper.newString(values, (byte) (coder & 0xFF), len, builder);
        uncheckedAdd(line);
      }

      position = file.getFilePointer();
      return true;
    } catch (IOException ex) {
      position = Long.MAX_VALUE;
    }

    return false;
  }

  @Override
  public void save() {
    throw new UnsupportedOperationException("Save is not supported");
  }

  private int readInt(RandomAccessFile file) throws IOException {
    try {
      return file.readInt();
    } catch (EOFException ex) {
      return -1;
    }
  }
}
