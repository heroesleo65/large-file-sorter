package org.example.base;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@EqualsAndHashCode(of = "bytes")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DataSize {

  private final long bytes;

  public long toBytes() {
    return bytes;
  }

  public long toKiloBytes() {
    return bytes >>> 10L;
  }

  public long toMegaBytes() {
    return bytes >>> 20L;
  }

  public long toGigaBytes() {
    return bytes >>> 30L;
  }

  public long toTeraBytes() {
    return bytes >>> 40L;
  }

  public static DataSize ofBytes(long bytes) {
    return new DataSize(bytes);
  }

  public static DataSize ofKiloBytes(long kiloBytes) {
    return new DataSize(kiloBytes << 10L);
  }

  public static DataSize ofMegaBytes(long megaBytes) {
    return new DataSize(megaBytes << 20L);
  }

  public static DataSize ofGigaBytes(long gigaBytes) {
    return new DataSize(gigaBytes << 30L);
  }

  public static DataSize ofTeraBytes(long teraBytes) {
    return new DataSize(teraBytes << 40L);
  }

  @Override
  public String toString() {
    return bytes + " byte(s)";
  }
}
