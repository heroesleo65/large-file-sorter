package org.example.utils;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public final class StringHelper {

  private static final int MIN_SIZE_RADIX_SORT = 3;

  private StringHelper() {
    throw new UnsupportedOperationException("StringHelper is utility");
  }

  public static void radixSort(String[] values, int fromIndex, int toIndex) {
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException(
          "fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")"
      );
    }

    if (fromIndex < 0) {
      throw new ArrayIndexOutOfBoundsException(fromIndex);
    }

    if (toIndex > values.length) {
      throw new ArrayIndexOutOfBoundsException(toIndex);
    }

    if (toIndex - fromIndex < MIN_SIZE_RADIX_SORT) {
      // Sorting small counts of elements
      Arrays.sort(values, fromIndex, toIndex, String::compareTo);
      return;
    }

    // TODO: add comments for explanation
    Deque<Long> buckets = new ArrayDeque<>();
    buckets.add(NumberHelper.getLong(fromIndex, toIndex));

    for (int length = 0; !buckets.isEmpty(); length++) {
      int position = length; // for lambda function

      long lastValue = buckets.peekLast();
      long value;
      do {
        //noinspection ConstantConditions
        value = buckets.pollFirst(); // value always exists

        int from = NumberHelper.getHiInt(value);
        int to = NumberHelper.getLowInt(value);

        Arrays.sort(values, from, to, (a, b) -> {
          if (position >= a.length()) {
            return position < b.length() ? -1 : 0;
          }
          if (position >= b.length()) {
            return 1;
          }
          return a.charAt(position) - b.charAt(position);
        });

        int prev = from;
        while (prev < to && values[prev].length() <= position) {
          prev++;
        }
        for (int i = prev + 1; i < to; i++) {
          if (values[i].charAt(position) != values[prev].charAt(position)) {
            if (i - prev > 1) {
              buckets.addLast(NumberHelper.getLong(prev, i));
            }
            prev = i;
          }
        }
        if (to - prev > 1) {
          buckets.addLast(NumberHelper.getLong(prev, to));
        }
      } while (lastValue != value);
    }
  }

  public static String newString(byte[] values, int count, StringBuilder buffer) {
    buffer.ensureCapacity(count);
    buffer.setLength(0);

    for (int i = 0; i < count; i += 2) {
      buffer.append((char) ((values[i] << 8) + (values[i + 1])));
    }
    return buffer.toString();
  }
}
