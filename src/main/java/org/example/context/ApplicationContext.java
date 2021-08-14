package org.example.context;

import java.io.IOException;
import java.io.OutputStream;
import org.example.io.StreamFactory;

public interface ApplicationContext {
  StreamFactory getStreamFactory();
  StringContext getStringContext();
  FileSystemContext getFileSystemContext();

  void sendSignal(IOException exception);

  default OutputStream getOutputStream(long fileId) throws IOException {
    var file = getFileSystemContext().getTemporaryFile(fileId);
    return getStreamFactory().getOutputStream(file);
  }
}
