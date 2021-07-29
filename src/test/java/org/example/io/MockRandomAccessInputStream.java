package org.example.io;

import java.io.ByteArrayInputStream;

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
      throw new IllegalArgumentException("pos must be greater than or equal to zero");
    }
    if (pos > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Too big value pos");
    }
    this.pos = (int) pos;
  }
}
