package org.example.context;

import java.io.IOException;
import java.io.OutputStream;
import org.example.io.OutputStreamFactory;

public interface ApplicationContext {
  OutputStreamFactory getOutputStreamFactory();
  StringContext getStringContext();
  FileSystemContext getFileSystemContext();

  default OutputStream getOutputStream(int fileId) throws IOException {
    var file = getFileSystemContext().getTemporaryFile(fileId);
    return getOutputStreamFactory().getOutputStream(file);
  }
}
