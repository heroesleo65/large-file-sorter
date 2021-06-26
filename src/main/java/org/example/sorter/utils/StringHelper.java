package org.example.sorter.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.function.Supplier;

public final class StringHelper {

  private static final String VALUE_OF_STRING_FIELD_NAME = "value";
  private static final String CODER_OF_STRING_FIELD_NAME = "coder";

  private static volatile Field VALUE_OF_STRING = null;
  private static volatile Field CODER_OF_STRING = null;
  private static volatile Constructor<String> STRING_CONSTRUCTOR_BY_VALUE_AND_CODER = null;

  private static volatile boolean SUPPORT_REFLECTION = true;

  private StringHelper() {
    throw new UnsupportedOperationException("StringHelper is utility");
  }

  @FunctionalInterface
  private interface Callable<T> {
    T call();
  }

  private static <T> T getLazyReflectionObject(Supplier<T> getter, Callable<T> setter) {
    if (!SUPPORT_REFLECTION) {
      return null;
    }

    if (getter.get() == null) {
      synchronized (StringHelper.class) {
        if (getter.get() == null) {
          if (setter.call() == null) {
            SUPPORT_REFLECTION = false;
          }
        }
      }
    }

    return getter.get();
  }

  private static Field getValueOfString() {
    return getLazyReflectionObject(
        () -> VALUE_OF_STRING,
        () -> VALUE_OF_STRING = ReflectionHelper.getField(String.class, VALUE_OF_STRING_FIELD_NAME)
    );
  }

  private static Field getCoderOfString() {
    return getLazyReflectionObject(
        () -> CODER_OF_STRING,
        () -> CODER_OF_STRING = ReflectionHelper.getField(String.class, CODER_OF_STRING_FIELD_NAME)
    );
  }

  private static Constructor<String> getStringConstructorByValueAndCoder() {
    return getLazyReflectionObject(
        () -> STRING_CONSTRUCTOR_BY_VALUE_AND_CODER,
        () -> STRING_CONSTRUCTOR_BY_VALUE_AND_CODER = ReflectionHelper.getConstructor(
            String.class, byte[].class, byte.class
        )
    );
  }

  public static void disableReflection() {
    SUPPORT_REFLECTION = false;
  }

  public static boolean checkSupportReflection() {
    if (!SUPPORT_REFLECTION) {
      return false;
    }

    if (getValueOfString() == null || getCoderOfString() == null ||
        getStringConstructorByValueAndCoder() == null) {
      SUPPORT_REFLECTION = false;
    }
    return SUPPORT_REFLECTION;
  }

  public static boolean hasSupportReflection() {
    return SUPPORT_REFLECTION;
  }

  public static byte[] getValueArray(String value) {
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
      SUPPORT_REFLECTION = false;
      return null;
    }
  }

  public static int getValueArray(String value, int offset, char[] chars, byte[] bytes) {
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

  public static byte getCoder(String value) {
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
      SUPPORT_REFLECTION = false;
      return -1;
    }
  }

  public static String newString(byte[] values, byte coder, int count, StringBuilder builder) {
    if (values == null) {
      return null;
    }

    if (coder < 0) {
      return newString(values, count, builder);
    }

    var constructor = getStringConstructorByValueAndCoder();
    if (constructor == null) {
      return newString(values, count, builder);
    }

    try {
      return constructor.newInstance(values, coder);
    } catch (Exception ex) {
      SUPPORT_REFLECTION = false;
      return newString(values, count, builder);
    }
  }

  private static String newString(byte[] values, int count, StringBuilder builder) {
    builder.ensureCapacity(count);
    builder.setLength(0);

    for (int i = 0; i < count; i += 2) {
      builder.append((char) ((values[i] << 8) + (values[i + 1])));
    }
    return builder.toString();
  }
}
