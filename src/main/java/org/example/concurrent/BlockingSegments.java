package org.example.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.LongStream;

// TODO: add description and comments
public class BlockingSegments implements BlockingBag {

  private static final int DEFAULT_CAPACITY = 10;
  private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE >>> 1;

  private final ReentrantLock lock = new ReentrantLock();
  private final Condition notEmpty = lock.newCondition();

  private volatile long[] elementData;
  private volatile int size;
  private final AtomicInteger count = new AtomicInteger();

  public BlockingSegments() {
    this.elementData = new long[DEFAULT_CAPACITY];
    this.size = 0;
  }

  public int countSegments() {
    return size;
  }

  public int countElements() {
    return count.get();
  }

  @Override
  public void add(long element) {
    if (element < 0) {
      throw new IllegalArgumentException("element must be greater than or equal to zero");
    }

    final var lock = this.lock;
    lock.lock();
    try {
      var elementData = this.elementData;
      int index = binarySearch(elementData, size, element);
      if (index >= 0) {
        // case: value exists in segments
        // example:
        // Insert 4 in ([3; 4] [6; 7] [11; 11]). Result: [3; 4] [6; 7] [11; 11]
        return;
      }

      index = -(index + 1);
      if (index < size) {
        if (index == 0) {
          // case: value in first segment
          // example:
          // Insert 2 in ([3; 4] [6; 7] [11; 11]). Result: [2; 4] [6; 7] [11; 11]
          if (element + 1 == elementData[0]) {
            elementData[0] = element;
          } else {
            // case: insert at 0 position
            // example:
            // Insert 1 in ([3; 4] [6; 7] [11; 11]). Result: [1; 1] [3; 4] [6; 7] [11; 11]
            insert(element, 0);
          }
        } else if (element + 1 == elementData[2 * index]) {
          if (elementData[2 * index - 1] + 1 == element) {
            // case: union of two segments
            // example:
            // Insert 5 in ([3; 4] [6; 7] [11; 11]). Result: [3; 7] [11; 11]
            elementData[2 * index - 1] = elementData[2 * index + 1];
            leftShift(/* position = */ index + 1, /* count = */ 1);
          } else {
            // case: value in left part of segment
            // example:
            // Insert 10 in ([3; 4] [6; 7] [11; 11]). Result: [3; 4] [6; 7] [10; 11]
            elementData[2 * index] = element;
          }
        } else if (elementData[2 * index - 1] + 1 == element) {
          // case: value in right part of segment
          // example:
          // Insert 8 in ([3; 4] [6; 7] [11; 11]). Result: [3; 4] [6; 8] [11; 11]
          elementData[2 * index - 1] = element;
        } else {
          // case: insert at index position
          // example:
          // Insert 9 in ([3; 4] [6; 7] [11; 11]). Result: [3; 4] [6; 7] [9; 9] [11; 11]
          insert(element, index);
        }
      } else if (size > 0 && elementData[2 * index - 1] + 1 == element) {
        // case: value in right part of last segment
        // example:
        // Insert 12 in ([3; 4] [6; 7] [11; 11]). Result: [3; 4] [6; 7] [11; 12]
        elementData[2 * index - 1] = element;
      } else {
        // case: insert at size position
        // example:
        // Insert 14 in ([3; 4] [6; 7] [11; 11]). Result: [3; 4] [6; 7] [11; 11] [14; 14]
        insert(element, size);
      }

      int c = count.getAndIncrement();
      if (c == 0) {
        notEmpty.signal();
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public LongStream takes(int countElements) throws InterruptedException {
    if (countElements <= 0) {
      return LongStream.empty();
    }

    var result = LongStream.empty();

    final var lock = this.lock;
    lock.lockInterruptibly();
    try {
      int resultCount = 0;
      while (resultCount < countElements) {
        final int remainingElements = countElements - resultCount;
        int c = count.getAndUpdate(value -> Math.max(0, value - remainingElements));
        while (c == 0) {
          notEmpty.await();
          c = count.getAndUpdate(value -> Math.max(0, value - remainingElements));
        }

        if (remainingElements < c) {
          c = remainingElements;
        }
        resultCount += c;

        final var elementData = this.elementData;
        for (int i = 0; c > 0; i++) {
          long elementsInSegment = elementData[2 * i + 1] - elementData[2 * i] + 1;
          if (elementsInSegment < c) {
            result = LongStream.concat(
                result, LongStream.rangeClosed(elementData[2 * i], elementData[2 * i + 1])
            );
            c -= elementsInSegment;
          } else if (elementsInSegment == c) {
            result = LongStream.concat(
                result, LongStream.rangeClosed(elementData[2 * i], elementData[2 * i + 1])
            );

            leftShift(/* position = */ i + 1, /* count = */ i + 1);
            break;
          } else {
            result = LongStream.concat(
                result, LongStream.range(elementData[2 * i], elementData[2 * i] + c)
            );

            elementData[2 * i] += c;
            if (i > 0) {
              leftShift(/* position = */ i, /* count = */ i);
            }
            break;
          }
        }
      }
    } finally {
      lock.unlock();
    }

    return result;
  }

  private void leftShift(int position, int count) {
    if (position < count || size < position) {
      return;
    }

    if (position < size) {
      System.arraycopy(
          elementData,
          /* srcPos = */ 2 * position,
          elementData,
          /* destPos = */ 2 * (position - count),
          /* length = */ 2 * (size - position)
      );
    }
    size -= count;
  }

  private void insert(long element, int position) {
    var elementData = this.elementData;
    if (2 * size == elementData.length) {
      elementData = grow(elementData.length + 2, position);
    } else {
      System.arraycopy(
          elementData,
          /* srcPos = */ 2 * position,
          elementData,
          /* destPos = */ 2 * position + 2,
          /* length = */ elementData.length - 2 * position - 2
      );
    }
    elementData[2 * position] = element;
    elementData[2 * position + 1] = element;
    size++;
  }

  private long[] grow(int minCapacity, int insertPosition) {
    int newLength = newCapacity(minCapacity);
    var copy = new long[newLength];

    int binValueInsertPosition = insertPosition << 1;

    // copy left part
    System.arraycopy(
        elementData,
        /* srcPos = */ 0,
        copy,
        /* destPos = */ 0,
        /* length = */ binValueInsertPosition
    );

    // copy right part
    System.arraycopy(
        elementData,
        /* srcPos = */ binValueInsertPosition,
        copy,
        /* destPos = */ binValueInsertPosition + 2,
        /* length = */ elementData.length - binValueInsertPosition
    );

    return (elementData = copy);
  }

  private int newCapacity(int minCapacity) {
    // overflow-conscious code
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if ((newCapacity & 1) == 1) {
      newCapacity++;
    }

    if (newCapacity - minCapacity <= 0) {
      if (minCapacity < 0) // overflow
        throw new OutOfMemoryError();
      return minCapacity;
    }
    if (newCapacity - MAX_ARRAY_SIZE <= 0) {
      return newCapacity;
    }
    throw new OutOfMemoryError();
  }

  private static int binarySearch(long[] a, int toIndex, long key) {
    int low = 0;
    int high = toIndex - 1;

    while (low <= high) {
      int mid = (low + high) >>> 1;
      long midLeftBoundVal = a[2 * mid];
      long midRightBoundVal = a[2 * mid + 1];

      if (midRightBoundVal < key)
        low = mid + 1;
      else if (key < midLeftBoundVal)
        high = mid - 1;
      else
        return mid; // key found
    }
    return -(low + 1);  // key not found.
  }
}
