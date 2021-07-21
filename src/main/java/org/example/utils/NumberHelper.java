package org.example.utils;

public final class NumberHelper {

  private static final long INT_IN_LONG_MASK = 0xFFFFFFFFL;

  private NumberHelper() {
    throw new UnsupportedOperationException("NumberHelper is utility");
  }

  public static long getLong(int a, int b) {
    return ((long) a << 32L) + ((long) b & INT_IN_LONG_MASK);
  }

  public static int getHiInt(long a) {
    return (int) ((a >>> 32L) & INT_IN_LONG_MASK);
  }

  public static int getLowInt(long a) {
    return (int) (a & INT_IN_LONG_MASK);
  }
}
