package org.example.sorter;

public interface OutputChunk extends Chunk {
  void setId(int id);

  void sort();
  void save();
  boolean add(String line);
}
