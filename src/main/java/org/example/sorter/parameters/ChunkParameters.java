package org.example.sorter.parameters;

import lombok.Getter;
import lombok.ToString;

@ToString(exclude = {"formula", "cacheAvailableChunks", "cacheChunkSize"})
public class ChunkParameters {

  private static final int DEFAULT_AVAILABLE_CHUNKS = 32;
  private static final int DEFAULT_CHUNK_SIZE = 64;

  private final Integer availableChunks;
  private final Integer chunkSize;
  @Getter
  private final int bufferSize;

  private final Long memorySize;
  private long avgStringSize = 5 * 1024 * 1024; // 5Mb

  private final ParametersFormula formula;

  private final CacheCalculate cacheCalculate;
  private volatile Integer cacheAvailableChunks;
  private volatile Integer cacheChunkSize;

  public ChunkParameters(
      Integer availableChunks,
      Integer chunkSize,
      int bufferSize,
      Long memorySize,
      ParametersFormula formula
  ) {
    this.availableChunks = availableChunks;
    this.chunkSize = chunkSize;
    this.bufferSize = bufferSize;
    this.memorySize = memorySize;
    this.formula = formula;

    if (chunkSize == null) {
      cacheCalculate = availableChunks == null
          ? new AllCacheCalculate()
          : new ChunkSizeCacheCalculate();
    } else {
      cacheCalculate = availableChunks == null
          ? new AvailableChunksCacheCalculate()
          : new NothingCacheCalculate();
    }
  }

  public int getAvailableChunks() {
    if (availableChunks != null) {
      return availableChunks;
    }

    if (memorySize == null) {
      return DEFAULT_AVAILABLE_CHUNKS;
    }

    if (cacheAvailableChunks == null) {
      cacheCalculate.calculate(memorySize, avgStringSize, bufferSize);
    }
    return cacheAvailableChunks;
  }

  public int getChunkSize() {
    if (chunkSize != null) {
      return chunkSize;
    }

    if (memorySize == null) {
      return DEFAULT_CHUNK_SIZE;
    }

    if (cacheChunkSize == null) {
      cacheCalculate.calculate(memorySize, avgStringSize, bufferSize);
    }
    return cacheChunkSize;
  }

  public void setAvgStringLength(long totalLength, long countLines) {
    avgStringSize = 2 * totalLength / countLines + 1;

    if (memorySize != null) {
      cacheCalculate.calculate(memorySize, avgStringSize, bufferSize);
    }
  }

  private abstract static class CacheCalculate {
    public abstract void calculate(long memorySize, long avgStringSize, long bufferSize);
  }

  private static class NothingCacheCalculate extends CacheCalculate {
    @Override
    public void calculate(long memorySize, long avgStringSize, long bufferSize) {
      // nothing
    }
  }

  private class AllCacheCalculate extends CacheCalculate {
    @Override
    public void calculate(long memorySize, long avgStringSize, long bufferSize) {
      cacheAvailableChunks = formula.getAvailableChunks(memorySize, avgStringSize, bufferSize);
      cacheChunkSize = formula.getAvailableChunks(memorySize, avgStringSize, bufferSize);
    }
  }

  private class AvailableChunksCacheCalculate extends CacheCalculate {
    @Override
    public void calculate(long memorySize, long avgStringSize, long bufferSize) {
      cacheAvailableChunks = formula.getAvailableChunks(
          memorySize, avgStringSize, chunkSize, bufferSize
      );
    }
  }

  private class ChunkSizeCacheCalculate extends CacheCalculate {
    @Override
    public void calculate(long memorySize, long avgStringSize, long bufferSize) {
      cacheChunkSize = formula.getChunkSize(
          memorySize, availableChunks, avgStringSize, bufferSize
      );
    }
  }
}
