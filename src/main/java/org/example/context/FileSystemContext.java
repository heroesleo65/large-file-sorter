package org.example.context;

import java.io.File;
import java.io.IOException;

public interface FileSystemContext {
  void createTemporaryDirectory() throws IOException;

  int nextTemporaryFile();
  File getTemporaryFile(int id);

  boolean exists(File file);
  boolean canRead(File file);
  boolean isFile(File file);

  boolean delete(File file);
}
