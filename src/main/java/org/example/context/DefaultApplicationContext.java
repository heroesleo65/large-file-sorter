package org.example.context;

import java.io.IOException;
import lombok.Getter;
import org.example.io.FileStreamFactory;
import org.example.io.StreamFactory;

@Getter
public class DefaultApplicationContext implements ApplicationContext {

  private final StreamFactory streamFactory;
  private final StringContext stringContext;
  private final FileSystemContext fileSystemContext;

  public DefaultApplicationContext(String prefixTemporaryDirectory, boolean supportReflection) {
    this(FileStreamFactory.getInstance(), prefixTemporaryDirectory, supportReflection);
  }

  public DefaultApplicationContext(
      StreamFactory streamFactory,
      String prefixTemporaryDirectory,
      boolean supportReflection
  ) {
    this.streamFactory = streamFactory;
    this.fileSystemContext = new DefaultFileSystemContext(prefixTemporaryDirectory);
    this.stringContext = new DefaultStringContext(supportReflection);
  }

  public DefaultApplicationContext(
      StreamFactory streamFactory,
      StringContext stringContext,
      FileSystemContext fileSystemContext
  ) {
    this.streamFactory = streamFactory;
    this.stringContext = stringContext;
    this.fileSystemContext = fileSystemContext;
  }

  @Override
  public void sendSignal(IOException exception) {
  }
}
