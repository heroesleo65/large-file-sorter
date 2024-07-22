package org.example.utils;

public final class StreamHelper {

  private StreamHelper() {
    throw new UnsupportedOperationException("StreamHelper is utility");
  }

  /**
   * Write integer to stream as
   * <a href="https://developers.google.com/protocol-buffers/docs/encoding">"Base 128 Varints"</a>.
   */
  public static int writeVarint32(byte[] buffer, int offset, int value) {
    do {
      if ((value & ~0x7F) == 0) {
        buffer[offset++] = (byte) value;
        return offset;
      }
      buffer[offset++] = (byte) ((value & 0x7F) | 0x80);
      value >>>= 7;
    } while (true);
  }
}
