package org.example.io;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;

public class MockRandomAccessInputStream extends ByteArrayInputStream
    implements RandomAccessInputStream {

  public MockRandomAccessInputStream(byte[] buf) {
    super(buf);
  }

  @Override
  public long length() {
    return buf.length;
  }

  @Override
  public synchronized long getFilePointer() {
    return pos;
  }

  @Override
  public synchronized void seek(long pos) {
    if (pos < 0) {
      throw new IllegalArgumentException("pos must be great than or equal to zero");
    }
    if (pos > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Too big value pos");
    }
    this.pos = (int) pos;
  }

  @Override
  public synchronized int readInt() throws IOException {
    int ch1 = this.read();
    int ch2 = this.read();
    int ch3 = this.read();
    int ch4 = this.read();
    if ((ch1 | ch2 | ch3 | ch4) < 0)
      throw new EOFException();
    return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
  }
}
