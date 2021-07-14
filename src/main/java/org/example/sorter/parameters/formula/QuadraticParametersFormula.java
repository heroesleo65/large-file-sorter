package org.example.sorter.parameters.formula;

import org.example.sorter.parameters.ParametersFormula;

public class QuadraticParametersFormula implements ParametersFormula {

  private static final long RESERVED_FOR_SYSTEM_MEMORY_SIZE = 50L * 1024L * 1024L; // 50Mb

  @Override
  public int getAvailableChunks(long memSize, long avgStrSize, int chunkSize, long bufSize) {
    return (int) ((memSize - RESERVED_FOR_SYSTEM_MEMORY_SIZE) / (avgStrSize * chunkSize + bufSize));
  }

  @Override
  public int getAvailableChunks(long memSize, long avgStrSize, long bufSize) {
    return (int) quadraticFormula(memSize, avgStrSize, bufSize);
  }

  @Override
  public int getChunkSize(long memSize, int availableChunks, long avgStrSize, long bufSize) {
    return (int) ((memSize / availableChunks - bufSize) / avgStrSize);
  }

  @Override
  public int getChunkSize(long memSize, long avgStrSize, long bufSize) {
    return (int) quadraticFormula(memSize, avgStrSize, bufSize);
  }

  private long quadraticFormula(long memSize, long avgStrSize, long bufSize) {
    long size = memSize - RESERVED_FOR_SYSTEM_MEMORY_SIZE;
    long d = (long) Math.sqrt(bufSize * bufSize + 4L * size * avgStrSize);
    return (d - bufSize) / (2 * avgStrSize);
  }
}
