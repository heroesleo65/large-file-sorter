package org.example.sorter.chunks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

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
      // ignore
    }

    clear(/* dirty = */false);
  }
}
