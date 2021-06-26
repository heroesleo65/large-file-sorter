package org.example.sorter.chunks;

import java.util.Arrays;
import org.example.sorter.Chunk;

public abstract class AbstractChunk implements Chunk {
  private static final String[] EMPTY = new String[0];

  protected String[] data;
  private int cursor;
  private int size;

  public AbstractChunk(int cap) {
    this.data = new String[cap];
    this.cursor = 0;
    this.size = 0;
  }

  @Override
  public void sort() {
    Arrays.sort(data, cursor, size, String::compareTo);
  }

  @Override
  public String pop() {
    if (size <= cursor) {
      cursor = 0;
      size = 0;

      if (!load()) {
        data = EMPTY;
        return null;
      }

      Arrays.fill(data, size, data.length, null); // for GC

      if (size == 0) {
        return null;
      }
    }
    return take(cursor++);
  }

  @Override
  public boolean add(String line) {
    if (size < data.length) {
      data[size++] = line;
      return true;
    }
    return false;
  }

  @Override
  public void clear(boolean dirty) {
    cursor = 0;
    size = 0;
    if (!dirty) {
      Arrays.fill(data, null); // for GC
    }
  }

  protected int getCurrentSize() {
    return size;
  }

  protected void uncheckedAdd(String line) {
    data[size++] = line;
  }

  private String take(int position) {
    var result = data[position];
    data[position] = null; // for GC
    return result;
  }
}
