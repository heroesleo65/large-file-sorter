package org.example.sorter;

public interface Chunk {
  int getId();

  void sort();

  boolean add(String line);

  String pop();

  boolean load();
  void save();

  void clear(boolean dirty);
}
