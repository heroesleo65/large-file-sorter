package org.example.sorter.chunks;

import java.util.Arrays;
import org.example.sorter.Chunk;
import org.example.sorter.utils.StringHelper;

public abstract class AbstractChunk implements Chunk {

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
    StringHelper.radixSort(data, cursor, size);
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

  protected int getCurrentCursor() {
    return cursor;
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
