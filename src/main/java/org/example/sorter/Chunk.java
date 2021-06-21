package org.example.sorter;

public interface Chunk {
  void sort();

  boolean add(String line);

  String pop();

  boolean load();
  void save();

  void clear(boolean dirty);
}
