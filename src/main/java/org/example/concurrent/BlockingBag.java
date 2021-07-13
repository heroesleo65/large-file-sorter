package org.example.concurrent;

import java.util.stream.IntStream;

public interface BlockingBag {
  void add(int element);
  IntStream takes(int countElements) throws InterruptedException;
}
