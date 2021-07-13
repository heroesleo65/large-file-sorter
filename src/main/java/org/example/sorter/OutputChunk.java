package org.example.sorter;

public interface OutputChunk extends Chunk {
  void setId(int id);

  void save();
  boolean add(String line);
}
