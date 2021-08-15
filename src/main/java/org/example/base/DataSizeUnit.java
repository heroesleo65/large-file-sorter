package org.example.base;

import java.util.function.LongFunction;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum DataSizeUnit {
  BYTES(DataSize::ofBytes, Long.MAX_VALUE),
  KILOBYTES(DataSize::ofKiloBytes, Long.MAX_VALUE >>> 10L),
  MEGABYTES(DataSize::ofMegaBytes, Long.MAX_VALUE >>> 20L),
  GIGABYTES(DataSize::ofGigaBytes, Long.MAX_VALUE >>> 30L),
  TERABYTES(DataSize::ofTeraBytes, Long.MAX_VALUE >>> 40L);

  private final LongFunction<DataSize> converter;
  private final long maxValue;

  public DataSize toDataSize(long value) {
    if (value <= maxValue) {
      return converter.apply(value);
    } else {
      throw new NumberFormatException();
    }
  }
}
