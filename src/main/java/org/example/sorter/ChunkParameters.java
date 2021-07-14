package org.example.sorter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString(exclude = {"cache"})
@RequiredArgsConstructor
public class ChunkParameters {

  private static final long RESERVED_FOR_SYSTEM_MEMORY_SIZE = 50 * 1024 * 1024;

  private final Integer availableChunks;
  private final Integer chunkSize;

  @Getter
  private final int bufferSize;
  private final Long memorySize;
  private long avgStringMemorySize = 5 * 1024 * 1024;
  private volatile Integer cache;

  public int getAvailableChunks() {
    return availableChunks != null ? availableChunks : calculateAvailableChunks();
  }

  public int getChunkSize() {
    return chunkSize != null ? chunkSize : calculateChunkSize();
  }

  public void setAvgStringLength(long totalLength, long countLines) {
    avgStringMemorySize = 2 * totalLength / countLines;
    if (2 * totalLength % countLines != 0) {
      avgStringMemorySize++;
    }

    if (memorySize != null) {
      cache = calculate();
    }
  }

  private int calculateAvailableChunks() {
    if (memorySize == null) {
      return 32;
    }

    if (cache == null) {
      cache = calculate();
    }
    return cache;
  }

  private int calculateChunkSize() {
    if (memorySize == null) {
      return 64;
    }

    if (cache == null) {
      cache = calculate();
    }
    return cache;
  }

  private int calculate() {
    long size = memorySize - RESERVED_FOR_SYSTEM_MEMORY_SIZE;
    long d = (long) Math.sqrt((long) bufferSize * bufferSize + 4L * size * avgStringMemorySize);
    int result = (int) ((d - bufferSize) / (2 * avgStringMemorySize));
    return Math.max(result, 3);
  }
}
