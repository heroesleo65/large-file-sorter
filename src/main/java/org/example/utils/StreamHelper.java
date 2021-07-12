package org.example.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import org.example.io.RandomAccessInputStream;

public final class StreamHelper {

  private StreamHelper() {
    throw new UnsupportedOperationException("StreamHelper is utility");
  }

  public static int readInt(RandomAccessInputStream stream) throws IOException {
    try {
      return stream.readInt();
    } catch (EOFException ex) {
      return -1;
    }
  }

  public static void writeInt(OutputStream stream, int value) throws IOException {
    stream.write((byte) ((value >>> 24) & 0xFF));
    stream.write((byte) ((value >>> 16) & 0xFF));
    stream.write((byte) ((value >>> 8) & 0xFF));
    stream.write((byte) ((value >>> 0) & 0xFF));
  }
}
