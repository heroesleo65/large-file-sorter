package org.example.utils;

import java.util.Arrays;

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
    var buckets = new RadixSortBounds();
    buckets.add(fromIndex, toIndex);

    RadixSortBound bound;
    for (int length = 0; !buckets.isEmpty(); length++) {
      final int position = length; // for lambda function

      final int lastRightBound = buckets.getLastRightBound();
      do {
        bound = buckets.poll();

        Arrays.sort(values, bound.from, bound.to, (a, b) -> {
          if (position >= a.length()) {
            return position < b.length() ? -1 : 0;
          }
          if (position >= b.length()) {
            return 1;
          }
          return a.charAt(position) - b.charAt(position);
        });

        int prev = bound.from;
        while (prev < bound.to && values[prev].length() <= position) {
          prev++;
        }
        for (int i = prev + 1; i < bound.to; i++) {
          if (values[i].charAt(position) != values[prev].charAt(position)) {
            if (i - prev > 1) {
              buckets.add(prev, i);
            }
            prev = i;
          }
        }
        if (bound.to - prev > 1) {
          buckets.add(prev, bound.to);
        }
      } while (lastRightBound != bound.to);
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

  private static class RadixSortBound {
    private final int from;
    private final int to;

    public RadixSortBound(int from, int to) {
      this.from = from;
      this.to = to;
    }
  }

  private static class RadixSortBounds {
    private long[] bounds;
    private int head;
    private int tail;

    public RadixSortBounds() {
      bounds = new long[1];
    }

    public void add(int from, int to) {
      bounds[tail] = NumberHelper.getLong(from, to);
      if (head == (tail = inc(tail, bounds.length))) {
        grow();
      }
    }

    public RadixSortBound poll() {
      var value = bounds[head];
      if (tail == (head = inc(head, bounds.length))) {
        head = 0;
        tail = 0;
      }
      return new RadixSortBound(NumberHelper.getHiInt(value), NumberHelper.getLowInt(value));
    }

    public int getLastRightBound() {
      var value = bounds[dec(tail, bounds.length)];
      return NumberHelper.getLowInt(value);
    }

    public boolean isEmpty() {
      return head == tail;
    }

    private void grow() {
      final int oldCapacity = bounds.length;
      int jump = (oldCapacity < 64) ? (oldCapacity + 2) : (oldCapacity >> 1);
      int newCapacity = oldCapacity + jump;

      final long[] bs = new long[newCapacity];
      System.arraycopy(bounds, head, bs, 0, bounds.length - head);
      System.arraycopy(bounds, 0, bs, bounds.length - head, head);

      head = 0;
      tail = bounds.length;
      bounds = bs;
    }

    private static int dec(int value, int module) {
      if (--value < 0) {
        value = module - 1;
      }
      return value;
    }

    private static int inc(int value, int module) {
      if (++value >= module) {
        value = 0;
      }
      return value;
    }
  }
}
