package org.example.sorter.chunks;

import java.util.Arrays;
import org.example.sorter.OutputChunk;
import org.example.utils.StringHelper;

public abstract class AbstractOutputChunk implements OutputChunk {

  protected String[] data;
  protected int cursor;
  protected int size;

  public AbstractOutputChunk(int cap) {
    this.data = new String[cap];
    this.cursor = 0;
    this.size = 0;
  }

  @Override
  public boolean isEmpty() {
    return cursor == size;
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
    cursor = 0;
    size = 0;
    Arrays.fill(data, null); // for GC
  }
}
