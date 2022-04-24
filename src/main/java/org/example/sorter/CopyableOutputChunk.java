package org.example.sorter;

import java.util.function.Predicate;

public interface CopyableOutputChunk extends OutputChunk {
  String copyWithSaveUtil(InputChunk inputChunk, Predicate<String> predicate);

  void copyAndSave(InputChunk inputChunk);
}
