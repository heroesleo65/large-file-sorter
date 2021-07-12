package org.example.context;

import lombok.Getter;
import org.example.io.FileOutputStreamFactory;
import org.example.io.OutputStreamFactory;

public class DefaultApplicationContext implements ApplicationContext {

  @Getter
  private final OutputStreamFactory outputStreamFactory;

  @Getter
  private final StringContext stringContext;

  @Getter
  private final FileSystemContext fileSystemContext;

  public DefaultApplicationContext(String prefixTemporaryDirectory, boolean supportReflection) {
    this(FileOutputStreamFactory.getInstance(), prefixTemporaryDirectory, supportReflection);
  }

  public DefaultApplicationContext(
      OutputStreamFactory outputStreamFactory,
      String prefixTemporaryDirectory,
      boolean supportReflection
  ) {
    this.outputStreamFactory = outputStreamFactory;
    this.fileSystemContext = new DefaultFileSystemContext(prefixTemporaryDirectory);
    this.stringContext = new DefaultStringContext(supportReflection);
  }

  public DefaultApplicationContext(
      OutputStreamFactory outputStreamFactory,
      StringContext stringContext,
      FileSystemContext fileSystemContext
  ) {
    this.outputStreamFactory = outputStreamFactory;
    this.stringContext = stringContext;
    this.fileSystemContext = fileSystemContext;
  }
}
