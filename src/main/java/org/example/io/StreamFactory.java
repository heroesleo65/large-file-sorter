package org.example.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;

public interface StreamFactory {
  OutputStream getOutputStream(File file) throws FileNotFoundException;

  RandomAccessInputStream getRandomAccessInputStream(File file) throws FileNotFoundException;

  BufferedReader getBufferedReader(Path path, Charset cs) throws IOException;
}
