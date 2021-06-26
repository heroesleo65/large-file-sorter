package org.example.sorter.chunks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import lombok.extern.log4j.Log4j2;
import org.example.sorter.utils.FileHelper;
import org.example.sorter.utils.StringHelper;

@Log4j2
public class TemporaryChunk extends AbstractChunk {
  private final File inputFile;
  private long position;
  private boolean fileExists;

  public TemporaryChunk(File inputFile, int chunkSize) {
    super(chunkSize);
    this.inputFile = inputFile;
    this.fileExists = inputFile.exists();
  }

  @Override
  public String pop() {
    var result = super.pop();
    if (result == null && fileExists) {
      fileExists = false;
      if (!FileHelper.safeDeleteFile(inputFile)) {
        log.error("Can't delete file '{}'", inputFile.getAbsolutePath());
      }
    }
    return result;
  }

  @Override
  public boolean load() {
    if (!fileExists || !inputFile.exists() || !inputFile.isFile()) {
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

        var len = FileHelper.readInt(file);
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
    } catch (FileNotFoundException ex) {
      fileExists = false;
      return false;
    } catch (IOException ex) {
      position = Long.MAX_VALUE;
    }

    return false;
  }

  @Override
  public void save() {
    throw new UnsupportedOperationException("Save is not supported");
  }
}
