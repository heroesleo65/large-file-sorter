package org.example.sorter.parameters;

import org.example.base.DataSize;

public final class DefaultParameters {

  public static final long RESERVED_FOR_SYSTEM_MEMORY_SIZE = DataSize.ofMegaBytes(50).toBytes();
  public static final long MIN_MEMORY_SIZE =
      RESERVED_FOR_SYSTEM_MEMORY_SIZE + DataSize.ofMegaBytes(10).toBytes();
  public static final long DEFAULT_AVG_STRING_MEMORY_SIZE = DataSize.ofMegaBytes(5).toBytes();
  public static final int DEFAULT_AVAILABLE_CHUNKS = 32;
  public static final int MIN_AVAILABLE_CHUNKS = 3;
  public static final int DEFAULT_CHUNK_SIZE = 64;
  public static final int MIN_CHUNK_SIZE = 1;
  public static final int DEFAULT_BUFFER_SIZE = 1024;

  private DefaultParameters() {
    throw new UnsupportedOperationException("DefaultParameters is utility");
  }
}
