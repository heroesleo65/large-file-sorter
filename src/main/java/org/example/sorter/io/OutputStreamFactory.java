package org.example.sorter.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;

public interface OutputStreamFactory {
  OutputStream getOutputStream(File file) throws FileNotFoundException;
  RandomAccessOutputStream getRandomAccessOutputStream(File file) throws FileNotFoundException;
}
