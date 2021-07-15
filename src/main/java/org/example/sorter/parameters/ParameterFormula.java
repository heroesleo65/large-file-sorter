package org.example.sorter.parameters;

public interface ParameterFormula {
  int getAvailableChunks(long memorySize, long avgStringSize, int chunkSize, long bufferSize);
  int getAvailableChunks(long memorySize, long avgStringSize, long bufferSize);

  int getChunkSize(long memorySize, int availableChunks, long avgStringSize, long bufferSize);
  int getChunkSize(long memorySize, long avgStringSize, long bufferSize);
}
