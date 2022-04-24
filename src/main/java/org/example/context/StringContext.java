package org.example.context;

public interface StringContext {
  boolean hasSupportReflection();

  byte getCoder(String value);

  byte[] getValueArray(String value);

  int getValueArray(String value, int offset, char[] chars, byte[] bytes);

  String createString(byte[] values, byte coder, int count, StringBuilder builder);
}
