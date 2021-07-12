package org.example.context;

import lombok.Getter;
import org.example.io.FileOutputStreamFactory;
import org.example.io.OutputStreamFactory;

public class DefaultApplicationContext implements ApplicationContext {

  @Getter
  private final OutputStreamFactory outputStreamFactory;

  @Getter
  private final StringContext stringContext;

  public DefaultApplicationContext(boolean supportReflection) {
    this(FileOutputStreamFactory.getInstance(), supportReflection);
  }

  public DefaultApplicationContext(
      OutputStreamFactory outputStreamFactory, boolean supportReflection
  ) {
    this.outputStreamFactory = outputStreamFactory;
    this.stringContext = new DefaultStringContext(supportReflection);
  }

  public DefaultApplicationContext(
      OutputStreamFactory outputStreamFactory, StringContext stringContext
  ) {
    this.outputStreamFactory = outputStreamFactory;
    this.stringContext = stringContext;
  }
}
