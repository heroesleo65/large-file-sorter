package org.example.sorter.chunks;

import java.util.Arrays;
import java.util.function.Predicate;
import org.example.sorter.InputChunk;
import org.example.sorter.OutputChunk;

public abstract class AbstractOutputChunk implements OutputChunk {

  protected String[] data;
  protected int size;

  public AbstractOutputChunk(int cap) {
    this.data = new String[cap];
    this.size = 0;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public void save() {
    if (size != 0) {
      save(data, 0, size);
      clear();
    }
  }

  protected abstract void save(String[] data, int from, int to);

  @Override
  public boolean add(String line) {
    if (size < data.length) {
      data[size++] = line;
      return true;
    }
    return false;
  }

  @Override
  public String copyWithSaveUtil(InputChunk inputChunk, Predicate<String> predicate) {
    if (inputChunk instanceof AbstractInputChunk) {
      var anotherChunk = (AbstractInputChunk) inputChunk;
      while (anotherChunk.nextLoad()) {
        for (int i = anotherChunk.cursor; i < anotherChunk.size; i++) {
          if (!predicate.test(anotherChunk.data[i])) {
            int count = i - anotherChunk.cursor;
            if (size + count < data.length) {
              System.arraycopy(anotherChunk.data, anotherChunk.cursor, data, size, count);
              size += count;
            } else {
              save();
              save(anotherChunk.data, anotherChunk.cursor, i);
            }
            anotherChunk.cursor = i + 1;
            return anotherChunk.data[i];
          }
        }
        save();
        save(anotherChunk.data, anotherChunk.cursor, anotherChunk.size);
        anotherChunk.cursor = anotherChunk.size;
      }
      anotherChunk.freeResources();
      return null;
    }

    var data = inputChunk.pop();
    while (data != null && predicate.test(data)) {
      add(data);
      data = inputChunk.pop();
    }
    return data;
  }

  @Override
  public void copyAndSave(InputChunk inputChunk) {
    if (inputChunk instanceof AbstractInputChunk) {
      save();

      var anotherChunk = (AbstractInputChunk) inputChunk;
      while (anotherChunk.nextLoad()) {
        save(anotherChunk.data, anotherChunk.cursor, anotherChunk.size);
        anotherChunk.cursor = anotherChunk.size;
      }
      anotherChunk.freeResources();
    } else {
      var data = inputChunk.pop();
      while (data != null) {
        add(data);
        data = inputChunk.pop();
      }

      save();
    }
  }

  protected void clear() {
    size = 0;
    Arrays.fill(data, null); // for GC
  }
}
