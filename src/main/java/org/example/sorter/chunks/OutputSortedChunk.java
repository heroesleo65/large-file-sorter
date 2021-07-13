package org.example.sorter.chunks;

import org.example.context.ApplicationContext;

public class OutputSortedChunk extends OutputUnsortedChunk {

  public OutputSortedChunk(int id, int chunkSize, ApplicationContext context) {
    super(id, chunkSize, context);
  }

  public OutputSortedChunk(int id, int chunkSize, int bufferSize, ApplicationContext context) {
    super(id, chunkSize, bufferSize, context);
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
