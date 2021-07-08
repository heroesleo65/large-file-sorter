package org.example.context;

import org.example.io.OutputStreamFactory;

public interface ApplicationContext {
  OutputStreamFactory getOutputStreamFactory();

  boolean hasSupportReflection();

  byte getCoder(String value);

  byte[] getValueArray(String value);
  int getValueArray(String value, int offset, char[] chars, byte[] bytes);

  String createString(byte[] values, byte coder, int count, StringBuilder builder);
}
