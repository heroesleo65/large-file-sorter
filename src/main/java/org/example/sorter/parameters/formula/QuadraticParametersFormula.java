package org.example.sorter.parameters.formula;

import org.example.sorter.parameters.DefaultParameters;
import org.example.sorter.parameters.ParametersFormula;

public class QuadraticParametersFormula implements ParametersFormula {

  @Override
  public int getAvailableChunks(long memSize, long avgStrSize, int chunkSize, long bufSize) {
    long size = memSize - DefaultParameters.RESERVED_FOR_SYSTEM_MEMORY_SIZE;
    return (int) (size / (avgStrSize * chunkSize + bufSize));
  }

  @Override
  public int getAvailableChunks(long memSize, long avgStrSize, long bufSize) {
    return (int) quadraticFormula(memSize, avgStrSize, bufSize);
  }

  @Override
  public int getChunkSize(long memSize, int availableChunks, long avgStrSize, long bufSize) {
    long size = memSize - DefaultParameters.RESERVED_FOR_SYSTEM_MEMORY_SIZE;
    return (int) ((size / availableChunks - bufSize) / avgStrSize);
  }

  @Override
  public int getChunkSize(long memSize, long avgStrSize, long bufSize) {
    return (int) quadraticFormula(memSize, avgStrSize, bufSize);
  }

  private long quadraticFormula(long memSize, long avgStrSize, long bufSize) {
    long size = memSize - DefaultParameters.RESERVED_FOR_SYSTEM_MEMORY_SIZE;
    long d = (long) Math.sqrt(bufSize * bufSize + 4L * size * avgStrSize);
    return (d - bufSize) / (2 * avgStrSize);
  }
}
