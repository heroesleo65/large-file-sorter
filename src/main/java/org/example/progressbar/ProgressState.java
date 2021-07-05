package org.example.progressbar;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import lombok.Setter;

public class ProgressState {

  @Getter
  private final String taskName;

  @Setter
  @Getter
  private volatile String extraMessage;

  //  0             start     current        max
  //  [===============|=========>             ]
  @Getter
  private volatile long start;
  private final AtomicLong current;

  @Getter
  private volatile long max;

  @Getter
  private volatile Instant startInstant;
  private Duration elapsedBeforeStart;

  @Getter
  private volatile boolean paused;

  public ProgressState(
      String taskName, long initialMax, long startFrom, Duration elapsedBeforeStart
  ) {
    this.taskName = taskName;
    this.max = initialMax;
    this.start = startFrom;
    this.current = new AtomicLong(startFrom);
    this.elapsedBeforeStart = elapsedBeforeStart;

    this.extraMessage = "";
    this.startInstant = Instant.now();
    this.paused = false;
  }

  public long getCurrent() {
    return current.get();
  }

  public void setMaxHint(long n) {
    max = n;
  }

  public void stepBy(long n) {
    current.addAndGet(n);
  }

  public void stepTo(long n) {
    current.set(n > 0 ? n : 0);
  }

  public synchronized void pause() {
    if (!paused) {
      paused = true;
      start = current.get();
      elapsedBeforeStart = elapsedBeforeStart.plus(Duration.between(startInstant, Instant.now()));
    }
  }

  public synchronized void resume() {
    if (paused) {
      paused = false;
      startInstant = Instant.now();
    }
  }
}
