package org.example.utils;

import java.io.IOException;
import java.io.OutputStream;

public final class StreamHelper {

  private StreamHelper() {
    throw new UnsupportedOperationException("StreamHelper is utility");
  }

  /**
   * Write integer to stream as "Base 128 Varints"
   * (https://developers.google.com/protocol-buffers/docs/encoding)
   */
  public static void writeVarint32(OutputStream stream, int value) throws IOException {
    do {
      if ((value & ~0x7F) == 0) {
        stream.write((byte) value);
        return;
      }
      stream.write((byte) ((value & 0x7F) | 0x80));
      value >>>= 7;
    } while (true);
  }
}
