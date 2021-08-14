package org.example.sorter;

import org.example.io.StringSerializer;
import org.example.sorter.chunks.ids.OutputChunkId;

public interface OutputChunk extends Chunk {
  void setId(OutputChunkId id);
  void setStringSerializer(StringSerializer serializer);

  void save();
  boolean add(String line);
}
