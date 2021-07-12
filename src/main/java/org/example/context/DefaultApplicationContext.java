package org.example.context;

import lombok.Getter;
import org.example.io.FileStreamFactory;
import org.example.io.StreamFactory;

public class DefaultApplicationContext implements ApplicationContext {

  @Getter
  private final StreamFactory streamFactory;

  @Getter
  private final StringContext stringContext;

  @Getter
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
}
