package org.example.sorter.chunks;

import java.io.File;

public class OutputSortedChunk extends UnsortedChunk {

  public OutputSortedChunk(File outputFile, int chunkSize) {
    super(outputFile, chunkSize);
  }

  public OutputSortedChunk(File outputFile, int chunkSize, int bufferSize) {
    super(outputFile, chunkSize, bufferSize);
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
