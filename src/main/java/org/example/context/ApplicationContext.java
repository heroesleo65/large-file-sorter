package org.example.context;

import java.io.IOException;
import java.io.OutputStream;
import org.example.io.StreamFactory;

public interface ApplicationContext {
  StreamFactory getStreamFactory();
  StringContext getStringContext();
  FileSystemContext getFileSystemContext();

  void sendIOExceptionEvent(IOException exception);

  default OutputStream getOutputStream(int fileId) throws IOException {
    var file = getFileSystemContext().getTemporaryFile(fileId);
    return getStreamFactory().getOutputStream(file);
  }
}
