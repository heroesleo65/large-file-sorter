package org.example.io;

import java.io.IOException;
import java.io.OutputStream;

public interface StringSerializer {
  void write(OutputStream stream, String[] data, int from, int to) throws IOException;
}
