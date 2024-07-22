package org.example.sorter.chunks;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import lombok.extern.log4j.Log4j2;
import org.example.context.ApplicationContext;
import org.example.io.StringSerializer;
import org.example.sorter.OutputChunk;
import org.example.sorter.chunks.ids.OutputChunkId;

@Log4j2
public abstract class AbstractOutputChunk implements OutputChunk {

  protected OutputChunkId id;
  protected StringSerializer serializer;
  protected final ApplicationContext context;

  protected String[] data;
  protected int size;

  protected AbstractOutputChunk(
      OutputChunkId id, int chunkSize, StringSerializer serializer, ApplicationContext context
  ) {
    this.id = id;
    this.serializer = serializer;
    this.context = context;

    this.data = new String[chunkSize];
    this.size = 0;
  }

  @Override
  public long getId() {
    return id.getId();
  }

  @Override
  public void setId(OutputChunkId id) {
    this.id = id;
  }

  @Override
  public void setStringSerializer(StringSerializer serializer) {
    this.serializer = serializer;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  protected OutputStream createOutputStream() throws IOException {
    return id.createOutputStream();
  }

  @Override
  public void save() {
    if (size != 0) {
      save(data, 0, size);
      clear();
    }
  }

  protected void save(String[] data, int from, int to) {
    try (var stream = createOutputStream()) {
      serializer.write(stream, data, from, to);
    } catch (IOException ex) {
      failSave(ex);
    }
  }

  protected void failSave(IOException ex) {
    log.error(() -> id.getMessageOnFailSave(), ex);
    context.sendSignal(ex);
  }

  @Override
  public boolean add(String line) {
    if (size < data.length) {
      data[size++] = line;
      return true;
    }
    return false;
  }

  protected void clear() {
    size = 0;
    Arrays.fill(data, null); // for GC
  }
}
