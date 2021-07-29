package org.example.io;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;

public interface RandomAccessInputStream extends Closeable {
  long length() throws IOException;
  long getFilePointer() throws IOException;
  void seek(long pos) throws IOException;

  int read() throws IOException;
  int read(byte[] buffer, int off, int len) throws IOException;

  /**
   * Read integer from stream as "Base 128 Varints"
   * (https://developers.google.com/protocol-buffers/docs/encoding)
   */
  default int readVarint32(final int firstByte) throws IOException {
    if ((firstByte & 0x80) == 0) {
      return firstByte;
    }

    int result = firstByte & 0x7F;
    int offset = 7;
    for (; offset < 32; offset += 7) {
      final int b = read();
      if (b == -1) {
        throw new EOFException();
      }
      result |= (b & 0x7F) << offset;
      if ((b & 0x80) == 0) {
        return result;
      }
    }
    // Keep reading up to 64 bits.
    for (; offset < 64; offset += 7) {
      final int b = read();
      if (b == -1) {
        throw new EOFException();
      }
      if ((b & 0x80) == 0) {
        return result;
      }
    }
    throw new EOFException(); // TODO: change to corrupted message exception
  }
}
