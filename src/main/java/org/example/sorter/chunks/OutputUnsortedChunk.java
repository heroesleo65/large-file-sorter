package org.example.sorter.chunks;

import java.util.Arrays;
import lombok.extern.log4j.Log4j2;
import org.example.context.ApplicationContext;
import org.example.sorter.SortableOutputChunk;

@Log4j2
public class OutputUnsortedChunk extends AbstractBinaryOutputChunk implements SortableOutputChunk {

  public OutputUnsortedChunk(int id, int chunkSize, ApplicationContext context) {
    super(id, chunkSize, context);
  }

  public OutputUnsortedChunk(int id, int chunkSize, int bufferSize, ApplicationContext context) {
    super(id, chunkSize, bufferSize, context);
  }

  @Override
  public void sort() {
    Arrays.sort(data, 0, size);
  }
}
