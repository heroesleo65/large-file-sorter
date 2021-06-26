package org.example.sorter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public final class ReflectionHelper {

  private ReflectionHelper() {
    throw new UnsupportedOperationException("ReflectionHelper is utility");
  }

  public static Field getField(Class<?> clazz, String name) {
    try {
      var field = clazz.getDeclaredField(name);
      field.setAccessible(true);
      return field;
    } catch (NoSuchFieldException ex) {
      return null;
    }
  }

  public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... parameterTypes) {
    try {
      var constructor = clazz.getDeclaredConstructor(parameterTypes);
      constructor.setAccessible(true);
      return constructor;
    } catch (NoSuchMethodException ex) {
      return null;
    }
  }
}
