package org.example.sorter.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RandomAccessFileOutputStream implements RandomAccessOutputStream {

  private final RandomAccessFile file;

  @Override
  public long length() throws IOException {
    return file.length();
  }

  @Override
  public long getFilePointer() throws IOException {
    return file.getFilePointer();
  }

  @Override
  public void seek(long pos) throws IOException {
    file.seek(pos);
  }

  @Override
  public int read() throws IOException {
    return file.read();
  }

  @Override
  public int readInt() throws IOException {
    return file.readInt();
  }

  @Override
  public int read(byte[] buffer, int off, int len) throws IOException {
    return file.read(buffer, off, len);
  }

  @Override
  public void close() throws IOException {
    file.close();
  }
}
