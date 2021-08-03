package org.example.sorter.chunks;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import lombok.extern.log4j.Log4j2;
import org.example.context.ApplicationContext;

@Log4j2
public class FinalOutputChunk extends AbstractOutputChunk {

  private final File outputFile;
  private final Charset charset;
  private final ApplicationContext context;
  private final byte[] newLineBytes;

  public FinalOutputChunk(
      File outputFile, Charset charset, int chunkSize, ApplicationContext context
  ) {
    super(chunkSize);
    this.outputFile = outputFile;
    this.charset = charset;
    this.newLineBytes = System.lineSeparator().getBytes(charset);
    this.context = context;
  }

  @Override
  public long getId() {
    throw new UnsupportedOperationException("FinalOutputChunk doesn't has id");
  }

  @Override
  public void setId(int id) {
    throw new UnsupportedOperationException("FinalOutputChunk doesn't has id");
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
  protected void save(String[] data, int from, int to) {
    try (var stream = context.getStreamFactory().getOutputStream(outputFile)) {
      save(stream, data, from, to);
    } catch (IOException ex) {
      failSaveToFile(ex);
    }
  }

  @Override
  protected void saveWithAdditionalData(String[] data, int from, int to) {
    try (var stream = context.getStreamFactory().getOutputStream(outputFile)) {
      if (size != 0) {
        save(stream, this.data, 0, size);
        clear();
      }
      save(stream, data, from, to);
    } catch (IOException ex) {
      failSaveToFile(ex);
    }
  }

  private void save(OutputStream stream, String[] data, int from, int to) throws IOException {
    for (int i = from; i < to; i++) {
      var bytes = data[i].getBytes(charset);
      stream.write(bytes);
      stream.write(newLineBytes);
    }
  }

  private void failSaveToFile(IOException ex) {
    log.error(() -> "Can't save data in file '" + outputFile + "'", ex);
    context.sendSignal(ex);
  }
}
