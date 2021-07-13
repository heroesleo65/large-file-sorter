package org.example.sorter.chunks;

import java.util.Arrays;
import org.example.sorter.InputChunk;

public abstract class AbstractInputChunk implements InputChunk {

  protected String[] data;
  protected int cursor;
  protected int size;

  public AbstractInputChunk(int cap) {
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
    if (size <= cursor) {
      cursor = 0;
      size = 0;

      var loaded = load();

      Arrays.fill(data, size, data.length, null); // for GC

      if (!loaded || size == 0) {
        return null;
      }
    }
    return take(cursor++);
  }

  private String take(int position) {
    var result = data[position];
    data[position] = null; // for GC
    return result;
  }
}
