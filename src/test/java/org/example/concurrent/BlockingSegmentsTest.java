package org.example.concurrent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BlockingSegmentsTest {

  @Test
  void addOneValue() {
    var segments = new BlockingSegments();
    segments.add(5);

    assertThat(segments.countSegments()).isEqualTo(1);
    assertThat(segments.countElements()).isEqualTo(1);
  }

  @Test
  void addTwoValues() {
    var segments = new BlockingSegments();
    segments.add(5);
    segments.add(7);

    assertThat(segments.countSegments()).isEqualTo(2);
    assertThat(segments.countElements()).isEqualTo(2);
  }

  @Test
  void addTwoNeighborValues() {
    var segments = new BlockingSegments();
    segments.add(5);
    segments.add(6);

    assertThat(segments.countSegments()).isEqualTo(1);
    assertThat(segments.countElements()).isEqualTo(2);
  }

  @Test
  void addTwoSameValues() {
    var segments = new BlockingSegments();
    segments.add(5);
    segments.add(5);

    assertThat(segments.countSegments()).isEqualTo(1);
    assertThat(segments.countElements()).isEqualTo(1);
  }

  @Test
  void addThreeValues() {
    var segments = new BlockingSegments();
    segments.add(5);
    segments.add(7);
    segments.add(6);

    assertThat(segments.countSegments()).isEqualTo(1);
    assertThat(segments.countElements()).isEqualTo(3);
  }

  @Test
  void addFiveValues() {
    var segments = new BlockingSegments();
    segments.add(10);
    segments.add(5);
    segments.add(6);
    segments.add(9);
    segments.add(7);

    assertThat(segments.countSegments()).isEqualTo(2);
    assertThat(segments.countElements()).isEqualTo(5);
  }

  @Test
  void takeOneValue() throws Exception {
    var segments = new BlockingSegments();
    segments.add(10);
    segments.add(5);
    segments.add(6);
    segments.add(9);
    segments.add(7);

    var result = segments.takes(1);
    assertThat(result).containsExactly(5L);

    assertThat(segments.countSegments()).isEqualTo(2);
    assertThat(segments.countElements()).isEqualTo(4);
  }

  @Test
  void takeTwoValues() throws Exception {
    var segments = new BlockingSegments();
    segments.add(10);
    segments.add(5);
    segments.add(6);
    segments.add(9);
    segments.add(7);

    var result = segments.takes(2);
    assertThat(result).containsExactly(5L, 6L);

    assertThat(segments.countSegments()).isEqualTo(2);
    assertThat(segments.countElements()).isEqualTo(3);
  }

  @Test
  void takeThreeValues() throws Exception {
    var segments = new BlockingSegments();
    segments.add(10);
    segments.add(5);
    segments.add(6);
    segments.add(9);
    segments.add(7);

    var result = segments.takes(3);
    assertThat(result).containsExactly(5L, 6L, 7L);

    assertThat(segments.countSegments()).isEqualTo(1);
    assertThat(segments.countElements()).isEqualTo(2);
  }

  @Test
  void takeFiveValues() throws Exception {
    var segments = new BlockingSegments();
    segments.add(10);
    segments.add(5);
    segments.add(6);
    segments.add(9);
    segments.add(7);

    var result = segments.takes(5);
    assertThat(result).containsExactly(5L, 6L, 7L, 9L, 10L);

    assertThat(segments.countSegments()).isEqualTo(0);
    assertThat(segments.countElements()).isEqualTo(0);
  }

  @Test
  void blockingWithTakeOnEmptySegments() throws Exception {
    final var segments = new BlockingSegments();

    var thread = new Thread(() -> {
      for (int i = 0; i < 5; i++) {
        try {
          Thread.sleep(1_000L);
        } catch (InterruptedException e) {
          // ignore
        }

        segments.add(i);
      }
    });
    thread.start();

    var result = segments.takes(5);
    assertThat(result).containsExactly(0L, 1L, 2L, 3L, 4L);

    assertThat(segments.countSegments()).isEqualTo(0);
    assertThat(segments.countElements()).isEqualTo(0);
  }

  @Test
  void blockingWithTake() throws Exception {
    final var segments = new BlockingSegments();
    segments.add(10);
    segments.add(5);
    segments.add(6);
    segments.add(9);
    segments.add(7);

    var thread = new Thread(() -> {
      for (int i = 0; i < 5; i++) {
        try {
          Thread.sleep(1_000L);
        } catch (InterruptedException e) {
          // ignore
        }

        segments.add(i + 11);
      }
    });
    thread.start();

    var result = segments.takes(8);
    assertThat(result).hasSize(8);

    thread.join();

    assertThat(segments.countSegments()).isEqualTo(1);
    assertThat(segments.countElements()).isEqualTo(2);
  }
}
