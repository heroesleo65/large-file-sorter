package org.example.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

public class MockOutputStream extends OutputStream {
  private final ByteArrayOutputStream outputStream;
  private final AtomicLong callsBeforeException;
  private final IOException exception;

  public MockOutputStream() {
    this(new ByteArrayOutputStream());
  }

  public MockOutputStream(ByteArrayOutputStream outputStream) {
    this(outputStream, Long.MAX_VALUE, new IOException());
  }

  public MockOutputStream(long callsBeforeException, IOException exception) {
    this(new ByteArrayOutputStream(), callsBeforeException, exception);
  }

  public MockOutputStream(
      ByteArrayOutputStream outputStream, long callsBeforeException, IOException exception
  ) {
    this.outputStream = outputStream;
    this.callsBeforeException = new AtomicLong(callsBeforeException);
    this.exception = exception;
  }

  public byte[] toByteArray() {
    return outputStream.toByteArray();
  }

  @Override
  public void write(int b) throws IOException {
    checkOnException();
    outputStream.write(b);
  }

  @Override
  public void write(byte[] b) throws IOException {
    checkOnException();
    outputStream.write(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    checkOnException();
    outputStream.write(b, off, len);
  }

  @Override
  public void flush() throws IOException {
    checkOnException();
    outputStream.flush();
  }

  @Override
  public void close() throws IOException {
    checkOnException();
  }

  private void checkOnException() throws IOException {
    if (callsBeforeException.decrementAndGet() <= 0) {
      throw exception;
    }
  }
}
