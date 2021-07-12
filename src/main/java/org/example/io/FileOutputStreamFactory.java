package org.example.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;

public class FileOutputStreamFactory implements OutputStreamFactory {
  @Getter(lazy = true)
  private static final OutputStreamFactory instance = new FileOutputStreamFactory();

  @Override
  public OutputStream getOutputStream(File file) throws FileNotFoundException {
    return new FileOutputStream(file, true);
  }

  @Override
  public RandomAccessOutputStream getRandomAccessOutputStream(File file)
      throws FileNotFoundException {
    return new RandomAccessFileOutputStream(new RandomAccessFile(file, "r"));
  }

  @Override
  public BufferedReader getBufferedReader(Path path, Charset cs) throws IOException {
    return Files.newBufferedReader(path, cs);
  }
}
