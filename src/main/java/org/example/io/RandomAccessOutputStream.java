package org.example.io;

import java.io.Closeable;
import java.io.IOException;

public interface RandomAccessOutputStream extends Closeable {
  long length() throws IOException;
  long getFilePointer() throws IOException;
  void seek(long pos) throws IOException;

  int read() throws IOException;
  int readInt() throws IOException;
  int read(byte[] buffer, int off, int len) throws IOException;
}
