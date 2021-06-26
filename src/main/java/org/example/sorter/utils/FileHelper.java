package org.example.sorter.utils;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public final class FileHelper {
  private FileHelper() {
    throw new UnsupportedOperationException("FileHelper is utility");
  }

  public static boolean deleteFile(File file) {
    if (file == null || !file.exists()) {
      return true;
    }

    return file.delete();
  }

  public static boolean safeDeleteFile(File file) {
    try {
      return deleteFile(file);
    } catch (Exception ex) {
      return false;
    }
  }

  public static File getTemporaryFile(File temporaryDirectory, int id) {
    return new File(temporaryDirectory, Integer.toString(id));
  }

  public static int readInt(RandomAccessFile file) throws IOException {
    try {
      return file.readInt();
    } catch (EOFException ex) {
      return -1;
    }
  }
}
