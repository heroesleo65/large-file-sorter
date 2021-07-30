package org.example.sorter.chunks;

import java.util.Arrays;
import java.util.function.Predicate;
import org.example.sorter.InputChunk;
import org.example.sorter.OutputChunk;

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
  public void save() {
    if (size != 0) {
      save(data, 0, size);

      // clear
      cursor = 0;
      size = 0;
      Arrays.fill(data, null); // for GC
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
        save();
        for (int i = anotherChunk.cursor; i < anotherChunk.size; i++) {
          if (!predicate.test(anotherChunk.data[i])) {
            save(anotherChunk.data, anotherChunk.cursor, i);
            anotherChunk.cursor = i + 1;
            return anotherChunk.data[i];
          }
        }
        save(anotherChunk.data, anotherChunk.cursor, anotherChunk.size);
        anotherChunk.cursor = anotherChunk.size;
      }
      Arrays.fill(anotherChunk.data, 0, anotherChunk.cursor, null); // for GC
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
}
