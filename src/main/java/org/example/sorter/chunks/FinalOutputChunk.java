package org.example.sorter.chunks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class FinalOutputChunk extends AbstractChunk {
  private final File outputFile;
  private final Charset charset;
  private final byte[] newLineBytes;

  public FinalOutputChunk(File outputFile, Charset charset, int chunkSize) {
    super(chunkSize);
    this.outputFile = outputFile;
    this.charset = charset;
    this.newLineBytes = System.lineSeparator().getBytes(charset);
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
  public boolean load() {
    throw new UnsupportedOperationException("Load is not supported");
  }

  @Override
  public void save() {
    try (var stream = new FileOutputStream(outputFile, true)) {
      for (int i = 0; i < getCurrentSize(); i++) {
        var bytes = data[i].getBytes(charset);
        stream.write(bytes);
        stream.write(newLineBytes);
      }
    } catch (IOException ex) {
      log.error(() -> "Can't save file '" + outputFile + "'", ex);
      // TODO: add processing error
    }

    clear(/* dirty = */false);
  }
}
