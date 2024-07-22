package org.example.utils;

import java.util.HashMap;
import java.util.Map;
import org.example.base.DataSize;
import org.example.base.DataSizeUnit;

public final class DataSizeHelper {

  private static final Map<Character, DataSizeUnit> DATA_SIZE_FUNCTIONS;

  static {
    DATA_SIZE_FUNCTIONS = new HashMap<>();
    DATA_SIZE_FUNCTIONS.put(' ', DataSizeUnit.BYTES);
    DATA_SIZE_FUNCTIONS.put('b', DataSizeUnit.BYTES);
    DATA_SIZE_FUNCTIONS.put('B', DataSizeUnit.BYTES);
    DATA_SIZE_FUNCTIONS.put('k', DataSizeUnit.KILOBYTES);
    DATA_SIZE_FUNCTIONS.put('K', DataSizeUnit.KILOBYTES);
    DATA_SIZE_FUNCTIONS.put('m', DataSizeUnit.MEGABYTES);
    DATA_SIZE_FUNCTIONS.put('M', DataSizeUnit.MEGABYTES);
    DATA_SIZE_FUNCTIONS.put('g', DataSizeUnit.GIGABYTES);
    DATA_SIZE_FUNCTIONS.put('G', DataSizeUnit.GIGABYTES);
    DATA_SIZE_FUNCTIONS.put('t', DataSizeUnit.TERABYTES);
    DATA_SIZE_FUNCTIONS.put('T', DataSizeUnit.TERABYTES);
  }

  private DataSizeHelper() {
    throw new UnsupportedOperationException("DataSizeHelper is utility");
  }

  public static DataSize parse(String text) {
    int i = 0;
    while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
      i++;
    }

    if (i == text.length()) {
      throw new NumberFormatException();
    }

    int start = i;
    while (i < text.length() && Character.isDigit(text.charAt(i))) {
      i++;
    }

    if (i == start) {
      throw new NumberFormatException();
    }

    long value = Long.parseLong(text, start, i, /* radix = */ 10);

    if (i < text.length()) {
      char c = text.charAt(i);
      DataSizeUnit dataSizeUnit = DATA_SIZE_FUNCTIONS.get(c);
      if (dataSizeUnit == null) {
        throw new NumberFormatException();
      }

      i++;
      if (i < text.length() && dataSizeUnit != DataSizeUnit.BYTES) {
        c = text.charAt(i);
        if (c == 'b' || c == 'B' || c == ' ') {
          i++;
        }
      }

      while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
        i++;
      }

      if (i != text.length()) {
        throw new NumberFormatException();
      }
      return dataSizeUnit.toDataSize(value);
    }

    return DataSizeUnit.BYTES.toDataSize(value);
  }
}
