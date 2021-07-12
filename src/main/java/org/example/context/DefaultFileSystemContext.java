package org.example.context;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultFileSystemContext implements FileSystemContext {

  private static volatile File tempDirectory;

  private final String prefix;
  private final Object lock = new Object();
  private final AtomicInteger number = new AtomicInteger();

  public DefaultFileSystemContext() {
    this(null);
  }

  public DefaultFileSystemContext(String prefix) {
    this.prefix = prefix;
  }

  @Override
  public void createTemporaryDirectory() throws IOException {
    if (tempDirectory == null) {
      synchronized (lock) {
        if (tempDirectory == null) {
          tempDirectory = Files.createTempDirectory(prefix).toFile();
          tempDirectory.deleteOnExit();
        }
      }
    }
  }

  @Override
  public int nextTemporaryFile() {
    return number.getAndIncrement();
  }

  @Override
  public File getTemporaryFile(int id) {
    return new File(tempDirectory, Integer.toString(id));
  }

  @Override
  public boolean exists(File file) {
    return file.exists();
  }

  @Override
  public boolean canRead(File file) {
    return file.canRead();
  }

  @Override
  public boolean isFile(File file) {
    return file.isFile();
  }

  @Override
  public boolean delete(File file) {
    try {
      return deleteFile(file);
    } catch (Exception ex) {
      return false;
    }
  }

  private static boolean deleteFile(File file) {
    if (file == null || !file.exists()) {
      return true;
    }

    return file.delete();
  }
}
