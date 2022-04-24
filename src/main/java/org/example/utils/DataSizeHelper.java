package org.example.utils;

import java.util.HashMap;
import java.util.Map;
import org.example.base.DataSize;
import org.example.base.DataSizeUnit;

public final class DataSizeHelper {

  private static final Map<Character, DataSizeUnit> dataSizeFunctions;

  static {
    dataSizeFunctions = new HashMap<>();
    dataSizeFunctions.put(' ', DataSizeUnit.BYTES);
    dataSizeFunctions.put('b', DataSizeUnit.BYTES);
    dataSizeFunctions.put('B', DataSizeUnit.BYTES);
    dataSizeFunctions.put('k', DataSizeUnit.KILOBYTES);
    dataSizeFunctions.put('K', DataSizeUnit.KILOBYTES);
    dataSizeFunctions.put('m', DataSizeUnit.MEGABYTES);
    dataSizeFunctions.put('M', DataSizeUnit.MEGABYTES);
    dataSizeFunctions.put('g', DataSizeUnit.GIGABYTES);
    dataSizeFunctions.put('G', DataSizeUnit.GIGABYTES);
    dataSizeFunctions.put('t', DataSizeUnit.TERABYTES);
    dataSizeFunctions.put('T', DataSizeUnit.TERABYTES);
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
      DataSizeUnit dataSizeUnit = dataSizeFunctions.get(c);
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
