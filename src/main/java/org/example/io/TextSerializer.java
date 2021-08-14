package org.example.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class TextSerializer implements StringSerializer {

  private final Charset charset;
  private final byte[] newLineBytes;

  public TextSerializer(Charset charset) {
    this.charset = charset;
    this.newLineBytes = System.lineSeparator().getBytes(charset);
  }

  @Override
  public void write(OutputStream stream, String[] data, int from, int to) throws IOException {
    for (int i = from; i < to; i++) {
      var bytes = data[i].getBytes(charset);
      stream.write(bytes);
      stream.write(newLineBytes);
    }
  }
}
