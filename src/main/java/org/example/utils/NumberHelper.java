package org.example.utils;

public final class NumberHelper {

  private NumberHelper() {
    throw new UnsupportedOperationException("NumberHelper is utility");
  }

  public static long getLong(int a, int b) {
    return ((long) a << 32L) + ((long) b & 0xFFFFFFFFL);
  }

  public static int getHiInt(long a) {
    return (int) ((a >>> 32L) & 0xFFFFFFFFL);
  }

  public static int getLowInt(long a) {
    return (int) (a & 0xFFFFFFFFL);
  }
}
