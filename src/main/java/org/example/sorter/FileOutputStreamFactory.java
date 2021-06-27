package org.example.sorter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import lombok.Getter;

public class FileOutputStreamFactory implements OutputStreamFactory {
  @Getter(lazy = true)
  private static final OutputStreamFactory instance = new FileOutputStreamFactory();

  @Override
  public OutputStream get(File file) throws FileNotFoundException {
    return new FileOutputStream(file, true);
  }
}
