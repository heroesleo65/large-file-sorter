package org.example.sorter.chunks;

import java.io.File;
import org.example.context.ApplicationContext;

public class OutputSortedChunk extends UnsortedChunk {

  public OutputSortedChunk(File outputFile, int chunkSize, ApplicationContext context) {
    super(outputFile, chunkSize, context);
  }

  public OutputSortedChunk(
      File outputFile, int chunkSize, int bufferSize, ApplicationContext context
  ) {
    super(outputFile, chunkSize, bufferSize, context);
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
