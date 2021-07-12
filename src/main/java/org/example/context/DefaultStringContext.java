package org.example.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.function.Supplier;
import org.example.utils.ReflectionHelper;
import org.example.utils.StringHelper;

public class DefaultStringContext implements StringContext {

  private static final String VALUE_OF_STRING_FIELD_NAME = "value";
  private static final String CODER_OF_STRING_FIELD_NAME = "coder";

  private volatile Field valueOfString = null;
  private volatile Field coderOfString = null;
  private volatile Constructor<String> stringConstructorByValueAndCoder = null;

  private volatile boolean supportReflection;

  public DefaultStringContext(boolean supportReflection) {
    this.supportReflection = supportReflection;

    // check support reflection
    this.supportReflection = (
        getValueOfString() != null &&
            getCoderOfString() != null &&
            getStringConstructorByValueAndCoder() != null
    );
  }

  @Override
  public boolean hasSupportReflection() {
    return supportReflection;
  }

  @Override
  public byte getCoder(String value) {
    if (value == null) {
      return 0;
    }

    var field = getCoderOfString();
    if (field == null) {
      return -1;
    }

    try {
      return field.getByte(value);
    } catch (IllegalAccessException e) {
      supportReflection = false;
      return -1;
    }
  }

  @Override
  public byte[] getValueArray(String value) {
    if (value == null) {
      return new byte[0];
    }

    var field = getValueOfString();
    if (field == null) {
      return null;
    }

    try {
      return (byte[]) field.get(value);
    } catch (IllegalAccessException e) {
      supportReflection = false;
      return null;
    }
  }

  @Override
  public int getValueArray(String value, int offset, char[] chars, byte[] bytes) {
    if (value == null || offset >= value.length()) {
      return 0;
    }

    var count = Integer.min(value.length() - offset, chars.length);
    value.getChars(offset, offset + count, chars, 0);

    for (int i = 0; i < count; i++) {
      bytes[2 * i] = (byte) ((chars[i] >>> 8) & 0xFF);
      bytes[2 * i + 1] = (byte) (chars[i] & 0xFF);
    }

    return count;
  }

  @Override
  public String createString(byte[] values, byte coder, int count, StringBuilder builder) {
    if (values == null) {
      return null;
    }

    if (coder < 0) {
      return StringHelper.newString(values, count, builder);
    }

    var constructor = getStringConstructorByValueAndCoder();
    if (constructor == null) {
      return StringHelper.newString(values, count, builder);
    }

    try {
      return constructor.newInstance(values, coder);
    } catch (Exception ex) {
      supportReflection = false;
      return StringHelper.newString(values, count, builder);
    }
  }

  @FunctionalInterface
  private interface Callable<T> {
    T call();
  }

  private <T> T getLazyReflectionObject(Supplier<T> getter, Callable<T> setter) {
    if (!supportReflection) {
      return null;
    }

    T object = getter.get();
    if (object == null) {
      synchronized (this) {
        if (getter.get() == null) {
          object = setter.call();
          if (object == null) {
            supportReflection = false;
          }
        }
      }
    }

    return object;
  }

  private Field getValueOfString() {
    return getLazyReflectionObject(
        () -> valueOfString,
        () -> valueOfString = ReflectionHelper.getField(String.class, VALUE_OF_STRING_FIELD_NAME)
    );
  }

  private Field getCoderOfString() {
    return getLazyReflectionObject(
        () -> coderOfString,
        () -> coderOfString = ReflectionHelper.getField(String.class, CODER_OF_STRING_FIELD_NAME)
    );
  }

  private Constructor<String> getStringConstructorByValueAndCoder() {
    return getLazyReflectionObject(
        () -> stringConstructorByValueAndCoder,
        () -> stringConstructorByValueAndCoder = ReflectionHelper.getConstructor(
            String.class, byte[].class, byte.class
        )
    );
  }
}
