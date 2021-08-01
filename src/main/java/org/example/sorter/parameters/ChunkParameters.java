package org.example.sorter.parameters;

import static org.example.sorter.parameters.DefaultParameters.COEFFICIENT_BUF_STRING;
import static org.example.sorter.parameters.DefaultParameters.DEFAULT_AVAILABLE_CHUNKS;
import static org.example.sorter.parameters.DefaultParameters.DEFAULT_CHUNK_SIZE;
import static org.example.sorter.parameters.DefaultParameters.MIN_AVAILABLE_CHUNKS;
import static org.example.sorter.parameters.DefaultParameters.MIN_CHUNK_SIZE;
import static org.example.sorter.parameters.DefaultParameters.RESERVED_FOR_SYSTEM_MEMORY_SIZE;

import lombok.Getter;
import lombok.ToString;
import org.example.sorter.SortState;

@ToString(exclude = "calculator")
public class ChunkParameters {

  private final int availableChunks;
  private final int chunkSize;
  @Getter
  private final int bufferSize;
  private final long memorySize;
  private final int threadsCount;

  private long totalStringSize;
  private long countStrings;
  private long avgStringSize;

  private final Calculator calculator;

  public ChunkParameters(
      Integer availableChunks, Integer chunkSize, int bufferSize, int threadsCount, Long memorySize
  ) {
    this.bufferSize = bufferSize;
    this.threadsCount = threadsCount;

    if (memorySize == null) {
      this.memorySize = 0;
      this.availableChunks = availableChunks != null ? availableChunks : DEFAULT_AVAILABLE_CHUNKS;
      this.chunkSize = chunkSize != null ? chunkSize : DEFAULT_CHUNK_SIZE;

      this.calculator = new NothingCalculator();
    } else {
      if (memorySize <= RESERVED_FOR_SYSTEM_MEMORY_SIZE) {
        throw new IllegalArgumentException();
      }
      this.memorySize = memorySize - RESERVED_FOR_SYSTEM_MEMORY_SIZE;

      if (availableChunks == null) {
        this.availableChunks = 0;
        if (chunkSize == null) {
          this.chunkSize = 0;
          this.calculator = new AllCalculator(threadsCount);
        } else {
          this.chunkSize = chunkSize;
          this.calculator = new AvailableChunksCalculator();
        }
      } else {
        this.availableChunks = availableChunks;
        if (chunkSize == null) {
          this.chunkSize = 0;
          this.calculator = new ChunkSizeCalculator();
        } else {
          this.chunkSize = chunkSize;
          this.calculator = new NothingCalculator();
        }
      }
    }
  }

  public int getAllowableChunks(SortState state) {
    return calculator.getAllowableChunks(state, avgStringSize);
  }

  public int getAvailableChunks(SortState state, int remainingChunks) {
    if (state == SortState.PARTITION_SORT) {
      return 1;
    }

    int chunks = calculator.getAllowableChunks(state, avgStringSize);
    if (remainingChunks < chunks) {
      return remainingChunks + 1;
    }

    int result = Math.max(chunks / threadsCount, MIN_AVAILABLE_CHUNKS) + (threadsCount >> 1);
    return Math.min(result, chunks);
  }

  public int getChunkSize(SortState state, int chunks) {
    return calculator.getChunkSize(state, chunks, avgStringSize);
  }

  public void addStringLength(long length) {
    totalStringSize += Character.BYTES * length;
    countStrings++;
    avgStringSize = totalStringSize / countStrings + 1;
  }

  private interface Calculator {
    int getAllowableChunks(SortState state, long stringSize);
    int getChunkSize(SortState state, int chunks, long stringSize);
  }

  private class NothingCalculator implements Calculator {

    @Override
    public int getAllowableChunks(SortState state, long stringSize) {
      return availableChunks;
    }

    @Override
    public int getChunkSize(SortState state, int chunks, long stringSize) {
      return chunkSize;
    }
  }

  private class AvailableChunksCalculator implements Calculator {

    private final long minOverflowStringSize;

    AvailableChunksCalculator() {
      minOverflowStringSize = (Long.MAX_VALUE - bufferSize) / chunkSize;
    }

    @Override
    public int getAllowableChunks(SortState state, long stringSize) {
      if (stringSize == 0 || stringSize > minOverflowStringSize) {
        return MIN_AVAILABLE_CHUNKS;
      }

      long chunks = memorySize / (stringSize * chunkSize + bufferSize);
      if (chunks < MIN_AVAILABLE_CHUNKS) {
        throw new IllegalArgumentException();
      }

      return state == SortState.PARTITION_SORT
          ? Math.max(1, (int) (chunks - 2L))
          : (int) chunks;
    }

    @Override
    public int getChunkSize(SortState state, int chunks, long stringSize) {
      return chunkSize;
    }
  }

  private abstract class AbstractChunkSizeCalculator implements Calculator {

    @Override
    public int getChunkSize(SortState state, int chunks, long stringSize) {
      if (stringSize == 0) {
        return MIN_CHUNK_SIZE;
      }

      if (state == SortState.PARTITION_SORT && 2 < chunks) {
        chunks -= 2;
      }

      long value = (memorySize / chunks - bufferSize) / stringSize;
      if (value < MIN_CHUNK_SIZE) {
        throw new IllegalArgumentException();
      }
      return (int) value;
    }
  }

  private class ChunkSizeCalculator extends AbstractChunkSizeCalculator implements Calculator {

    @Override
    public int getAllowableChunks(SortState state, long stringSize) {
      return availableChunks;
    }
  }

  private class AllCalculator extends AbstractChunkSizeCalculator implements Calculator {

    private final int threadsCount;

    AllCalculator(int threadsCount) {
      this.threadsCount = threadsCount;
    }

    @Override
    public int getAllowableChunks(SortState state, long stringSize) {
      if (state == SortState.PARTITION_SORT) {
        if (stringSize == 0) {
          return 1;
        }

        long chunks = memorySize / (DEFAULT_CHUNK_SIZE * stringSize + bufferSize);
        if (threadsCount <= chunks) {
          return threadsCount;
        }

        chunks = memorySize / (stringSize + bufferSize);
        if (threadsCount <= chunks) {
          return threadsCount <= 2 * chunks ? threadsCount : (threadsCount >> 1) + 1;
        }
        if (chunks < 1) {
          throw new IllegalArgumentException();
        }
        return (int) chunks;
      }

      if (stringSize == 0) {
        return MIN_AVAILABLE_CHUNKS;
      }

      long chunks = memorySize / (Math.max(stringSize, COEFFICIENT_BUF_STRING) + bufferSize);
      if (chunks < MIN_AVAILABLE_CHUNKS) {
        chunks = memorySize / (DEFAULT_CHUNK_SIZE * stringSize + bufferSize);
        if (chunks < MIN_AVAILABLE_CHUNKS) {
          chunks = memorySize / (stringSize + bufferSize);
          if (chunks < MIN_AVAILABLE_CHUNKS) {
            throw new IllegalArgumentException();
          }
        }
      }

      return (int) chunks;
    }
  }
}
