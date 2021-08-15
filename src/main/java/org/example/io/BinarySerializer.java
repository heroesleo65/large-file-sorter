package org.example.io;

import java.io.IOException;
import java.io.OutputStream;
import lombok.RequiredArgsConstructor;
import org.example.context.StringContext;
import org.example.utils.StreamHelper;

@RequiredArgsConstructor
public class BinarySerializer implements StringSerializer {

  private final int bufferSize;
  private final StringContext context;

  @Override
  public void write(OutputStream stream, String[] data, int from, int to) throws IOException {
    // MetaData:
    // first byte - coder
    // 2-6 bytes - decoded len of string as 128 Base varint
    final var metaData = new byte[6];

    // buffers
    char[] chars = null;
    byte[] bytes = null;

    for (int i = from; i < to; i++) {
      var line = data[i];

      metaData[0] = context.getCoder(line);

      var value = context.getValueArray(line);
      if (value == null) {
        if (chars == null) {
          chars = new char[bufferSize >> 2];
        }
        if (bytes == null) {
          bytes = new byte[bufferSize >> 1];
        }
        int len = StreamHelper.writeVarint32(metaData, 1, 2 * line.length());
        stream.write(metaData, 0, len);
        writeData(stream, line, chars, bytes);
      } else {
        int len = StreamHelper.writeVarint32(metaData, 1, value.length);
        stream.write(metaData, 0, len);
        stream.write(value);
      }
    }
  }

  private void writeData(OutputStream stream, String line, char[] chars, byte[] bytes)
      throws IOException {
    int count;
    int offset = 0;
    while ((count = context.getValueArray(line, offset, chars, bytes)) > 0) {
      offset += count;
      stream.write(bytes, 0, 2 * count);
    }
  }
}
