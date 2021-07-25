package org.example.utils;

public final class StringHelper {

  private StringHelper() {
    throw new UnsupportedOperationException("StringHelper is utility");
  }

  public static String newString(byte[] values, int count, StringBuilder buffer) {
    buffer.ensureCapacity(count);
    buffer.setLength(0);

    for (int i = 0; i < count; i += 2) {
      buffer.append((char) ((values[i] << 8) + (values[i + 1])));
    }
    return buffer.toString();
  }
}
