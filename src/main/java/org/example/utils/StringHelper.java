package org.example.utils;

import java.util.Arrays;
import java.util.Comparator;

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
    var buckets = new RadixSortBounds(fromIndex, toIndex);
    for (int length = 0; !buckets.isEmpty(); length++) {
      final int position = length; // for lambda function

      final int lastRightBound = buckets.peekLastValue();
      int from, to;
      do {
        from = buckets.poll();
        to = buckets.poll();

        // find and skip strings with empty symbol at "position"-th position
        for (int i = from; i < to; i++) {
          if (values[i].length() <= position) {
            var temp = values[from];
            values[from] = values[i];
            values[i] = temp;
            from++;
          }
        }

        Arrays.sort(values, from, to, Comparator.comparingInt(a -> a.charAt(position)));

        char prevChar = values[from].charAt(position);
        for (int i = from + 1; i < to; i++) {
          char curChar = values[i].charAt(position);
          if (curChar == prevChar) {
            for (i++; i < to; i++) {
              curChar = values[i].charAt(position);
              if (curChar != prevChar) {
                break;
              }
            }
            buckets.offer(from);
            buckets.offer(i);
          }

          prevChar = curChar;
          from = i;
        }
      } while (lastRightBound != to);
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

  private static class RadixSortBounds {
    private int[] bounds;
    private int head;
    private int tail;

    public RadixSortBounds(int from, int to) {
      bounds = new int[16];
      bounds[0] = from;
      bounds[1] = to;
      head = 0;
      tail = 2;
    }

    public void offer(int value) {
      bounds[tail] = value;
      if (head == (tail = inc(tail, bounds.length))) {
        grow();
      }
    }

    public int poll() {
      var value = bounds[head];
      head = inc(head, bounds.length);
      return value;
    }

    public int peekLastValue() {
      return bounds[dec(tail, bounds.length)];
    }

    public boolean isEmpty() {
      return head == tail;
    }

    private void grow() {
      final int oldCapacity = bounds.length;
      int jump = (oldCapacity < 64) ? (oldCapacity + 2) : (oldCapacity >> 1);
      int newCapacity = oldCapacity + jump;

      final int[] bs = new int[newCapacity];
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
