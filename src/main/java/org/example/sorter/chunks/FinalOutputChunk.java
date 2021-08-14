package org.example.sorter.chunks;

import lombok.extern.log4j.Log4j2;
import org.example.context.ApplicationContext;
import org.example.io.StringSerializer;
import org.example.sorter.chunks.ids.OutputChunkId;

@Log4j2
public class FinalOutputChunk extends AbstractCopyableOutputChunk {

  public FinalOutputChunk(
      OutputChunkId id, int chunkSize, StringSerializer serializer, ApplicationContext context
  ) {
    super(id, chunkSize, serializer, context);
  }

  @Override
  public long getId() {
    throw new UnsupportedOperationException("FinalOutputChunk doesn't has id");
  }
}
