package org.example.sorter.parameters.formula;

import static org.example.sorter.parameters.DefaultParameters.MIN_AVAILABLE_CHUNKS;
import static org.example.sorter.parameters.DefaultParameters.MIN_CHUNK_SIZE;
import static org.example.sorter.parameters.DefaultParameters.RESERVED_FOR_SYSTEM_MEMORY_SIZE;

import org.example.sorter.parameters.ParameterFormula;

public class QuadraticParameterFormula implements ParameterFormula {

  @Override
  public int getAvailableChunks(long memSize, long avgStrSize, int chunkSize, long bufSize) {
    long size = memSize - RESERVED_FOR_SYSTEM_MEMORY_SIZE;
    int result = (int) (size / (avgStrSize * chunkSize + bufSize));
    return Math.max(result, MIN_AVAILABLE_CHUNKS);
  }

  @Override
  public int getAvailableChunks(long memSize, long avgStrSize, long bufSize) {
    int result = (int) quadraticFormula(memSize, avgStrSize, bufSize);
    return Math.max(result, MIN_AVAILABLE_CHUNKS);
  }

  @Override
  public int getChunkSize(long memSize, int availableChunks, long avgStrSize, long bufSize) {
    long size = memSize - RESERVED_FOR_SYSTEM_MEMORY_SIZE;
    int result = (int) ((size / availableChunks - bufSize) / avgStrSize);
    return Math.max(result, MIN_CHUNK_SIZE);
  }

  @Override
  public int getChunkSize(long memSize, long avgStrSize, long bufSize) {
    int result = (int) quadraticFormula(memSize, avgStrSize, bufSize);
    return Math.min(result, MIN_CHUNK_SIZE);
  }

  private long quadraticFormula(long memSize, long avgStrSize, long bufSize) {
    long size = memSize - RESERVED_FOR_SYSTEM_MEMORY_SIZE;
    long d = (long) Math.sqrt(bufSize * bufSize + 4L * size * avgStrSize);
    return (d - bufSize) / (2 * avgStrSize);
  }
}
