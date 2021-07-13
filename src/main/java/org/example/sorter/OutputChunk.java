package org.example.sorter;

public interface OutputChunk extends Chunk {
  void sort();
  void save();
  boolean add(String line);
}
