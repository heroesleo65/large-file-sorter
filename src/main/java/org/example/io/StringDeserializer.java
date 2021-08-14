package org.example.io;

import java.io.IOException;

public interface StringDeserializer {
  int read(RandomAccessInputStream inputStream, String[] data, int offset) throws IOException;
}
