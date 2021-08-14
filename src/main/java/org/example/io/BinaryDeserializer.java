package org.example.io;

import java.io.EOFException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.example.context.ApplicationContext;

@RequiredArgsConstructor
public class BinaryDeserializer implements StringDeserializer {

  private final ApplicationContext context;

  @Override
  public int read(RandomAccessInputStream inputStream, String[] data, int offset)
      throws IOException {

    // MetaData:
    // first byte - coder
    // second byte - first decoded byte from len of string as 128 Base varint
    final var metaData = new byte[2];

    final var buffer = new StringBuilder(0);

    var bytes = new byte[0];
    while (offset < data.length) {
      int count = inputStream.read(metaData, 0, 2);
      if (count <= 0) {
        return offset;
      }

      if (count < 2) {
        throw new EOFException();
      }

      int len = inputStream.readVarint32(metaData[1]);
      if (len < 0) {
        throw new IOException("Negative length was loaded");
      }

      // TODO: add comments for explanation
      if (context.getStringContext().hasSupportReflection() || bytes.length < len) {
        bytes = new byte[len];
      }
      if (inputStream.read(bytes, 0, len) != len) {
        throw new EOFException();
      }

      data[offset++] = context.getStringContext().createString(bytes, metaData[0], len, buffer);
    }

    return offset;
  }
}
