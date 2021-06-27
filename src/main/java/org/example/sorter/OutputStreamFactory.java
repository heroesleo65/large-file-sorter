package org.example.sorter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;

@FunctionalInterface
public interface OutputStreamFactory {
  OutputStream get(File file) throws FileNotFoundException;
}
