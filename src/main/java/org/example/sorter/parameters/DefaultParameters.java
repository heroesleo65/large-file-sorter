package org.example.sorter.parameters;

public final class DefaultParameters {

  public static final long RESERVED_FOR_SYSTEM_MEMORY_SIZE = 50L * 1024L * 1024L; // 50Mb
  public static final long AVG_STRING_MEMORY_SIZE = 5L * 1024L * 1024L; // 5Mb
  public static final int AVAILABLE_CHUNKS = 32;
  public static final int CHUNK_SIZE = 64;

  private DefaultParameters() {
    throw new UnsupportedOperationException("DefaultParameters is utility");
  }
}
