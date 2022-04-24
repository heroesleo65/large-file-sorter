package org.example.concurrent;

import java.util.stream.LongStream;

public interface BlockingBag {
  void add(long element);

  LongStream takes(int countElements) throws InterruptedException;
}
