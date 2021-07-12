package org.example.context;

import org.example.io.OutputStreamFactory;

public interface ApplicationContext {
  OutputStreamFactory getOutputStreamFactory();
  StringContext getStringContext();
}
