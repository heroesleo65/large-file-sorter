package org.example.sorter.chunks;

import java.util.Arrays;
import org.example.sorter.InputChunk;

public abstract class AbstractInputChunk implements InputChunk {

  protected String[] data;
  protected int cursor;
  protected int size;

  protected AbstractInputChunk(int cap) {
    this.data = new String[cap];
    this.cursor = 0;
    this.size = 0;
  }

  @Override
  public boolean isEmpty() {
    return cursor == size;
  }

  @Override
  public String pop() {
    return nextLoad() ? take(cursor++) : null;
  }

  boolean nextLoad() {
    if (cursor < size) {
      return true;
    }

    cursor = 0;
    size = 0;

    var loaded = load();

    Arrays.fill(data, size, data.length, null); // for GC

    return loaded && size != 0;
  }

  protected void freeResources() {
    cursor = 0;
    size = 0;
    Arrays.fill(data, null); // for GC
  }

  private String take(int position) {
    var result = data[position];
    data[position] = null; // for GC
    return result;
  }
}
