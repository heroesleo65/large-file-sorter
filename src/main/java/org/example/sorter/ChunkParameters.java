package org.example.sorter;

import lombok.Data;

@Data
public class ChunkParameters {
  private final int availableChunks;
  private final int chunkSize;
  private final int bufferSize;
}
