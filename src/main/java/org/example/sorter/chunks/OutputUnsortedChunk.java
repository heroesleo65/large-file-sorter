package org.example.sorter.chunks;

import java.util.Arrays;
import java.util.Comparator;
import lombok.extern.log4j.Log4j2;
import org.example.context.ApplicationContext;
import org.example.sorter.SortableOutputChunk;

@Log4j2
public class OutputUnsortedChunk extends AbstractBinaryOutputChunk implements SortableOutputChunk {

  private final Comparator<String> comparator;

  public OutputUnsortedChunk(
      int id, int chunkSize, Comparator<String> comparator, ApplicationContext context
  ) {
    super(id, chunkSize, context);
    this.comparator = comparator;
  }

  public OutputUnsortedChunk(
      int id,
      int chunkSize,
      int bufferSize,
      Comparator<String> comparator,
      ApplicationContext context
  ) {
    super(id, chunkSize, bufferSize, context);
    this.comparator = comparator;
  }

  @Override
  public void sort() {
    Arrays.sort(data, 0, size, comparator);
  }
}
