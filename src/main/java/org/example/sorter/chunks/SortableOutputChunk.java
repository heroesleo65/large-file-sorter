package org.example.sorter.chunks;

import java.util.Arrays;
import java.util.Comparator;
import lombok.extern.log4j.Log4j2;
import org.example.context.ApplicationContext;
import org.example.io.StringSerializer;
import org.example.sorter.chunks.ids.OutputChunkId;

@Log4j2
public class SortableOutputChunk extends AbstractOutputChunk {

  private final Comparator<String> comparator;

  public SortableOutputChunk(
      OutputChunkId id,
      int chunkSize,
      StringSerializer serializer,
      Comparator<String> comparator,
      ApplicationContext context
  ) {
    super(id, chunkSize, serializer, context);
    this.comparator = comparator;
  }

  @Override
  public void save() {
    Arrays.sort(data, 0, size, comparator);
    super.save();
  }
}
