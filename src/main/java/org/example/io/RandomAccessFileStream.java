package org.example.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class RandomAccessFileStream extends RandomAccessFile implements RandomAccessInputStream {

  public RandomAccessFileStream(File file, String mode) throws FileNotFoundException {
    super(file, mode);
  }
}
