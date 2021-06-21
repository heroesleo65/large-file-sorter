package org.example.sorter.chunks;

import java.io.File;

public class OutputSortedChunk extends UnsortedChunk {

  public OutputSortedChunk(File outputFile, int chunkSize) {
    super(outputFile, chunkSize);
  }

  @Override
  public boolean add(String line) {
    if (!super.add(line)) {
      save();
      return super.add(line);
    }
    return true;
  }
}
