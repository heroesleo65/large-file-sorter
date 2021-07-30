package org.example.sorter;

import java.util.function.Predicate;

public interface OutputChunk extends Chunk {
  void setId(int id);

  void save();
  boolean add(String line);

  String copyWithSaveUtil(InputChunk inputChunk, Predicate<String> predicate);
  void copyAndSave(InputChunk inputChunk);
}
