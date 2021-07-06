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

  @Getter
  private volatile boolean paused;

  private final AtomicLong current;

  @Getter
  private volatile long max;

  private volatile Instant processInstant;
  private volatile Instant speedInstant;

  private volatile Duration processElapsed;
  private volatile Duration speedElapsed;

  private volatile ProgressSpeed speed;
  private final AtomicLong speedDelta;

  private final Object lock = new Object();

  public ProgressState(String taskName, long initialMax, long startFrom) {
    this.taskName = taskName;
    this.max = initialMax;
    this.current = new AtomicLong(startFrom);

    this.extraMessage = "";
    this.paused = false;

    this.processElapsed = Duration.ZERO;
    this.speedElapsed = Duration.ZERO;

    this.processInstant = Instant.now();
    this.speedInstant = this.processInstant;

    this.speed = new AvgProgressSpeed();
    this.speedDelta = new AtomicLong(0L);
  }

  public long getCurrent() {
    return current.get();
  }

  public void setMaxHint(long n) {
    max = n;
  }

  public void stepBy(long n) {
    current.addAndGet(n);
    speedDelta.addAndGet(n);
  }

  public void stepTo(long n) {
    var oldValue = current.getAndSet(n > 0 ? n : 0);
    if (n > oldValue) {
      speedDelta.addAndGet(n - oldValue);
    }
  }

  public Duration getEta() {
    long max = this.max;
    if (max < 0) {
      return null;
    }

    long remainingProgress = max - current.get();
    if (remainingProgress <= 0) {
      return Duration.ZERO;
    }

    var currentSpeed = this.speed;
    var elapsed = getElapsed(speedElapsed, speedInstant, paused);

    long elapsedSeconds = elapsed.toSeconds();
    if (elapsedSeconds < 10L) {
      return currentSpeed.getEta(remainingProgress);
    }

    synchronized (lock) {
      speedElapsed = Duration.ZERO;
      speedInstant = Instant.now();
    }

    currentSpeed = new CurrentProgressSpeed(elapsedSeconds, speedDelta.getAndSet(0L));
    speed = currentSpeed;
    return currentSpeed.getEta(remainingProgress);
  }

  public double getSpeed() {
    var currentSpeed = this.speed;
    var delta = this.speedDelta.get();
    var elapsed = getElapsed(speedElapsed, speedInstant, paused);

    long elapsedSeconds = elapsed.toSeconds();
    if (elapsedSeconds == 0) {
      return currentSpeed.getSpeed();
    }

    return delta > 0 ? (double) delta / elapsedSeconds : 0;
  }

  public Duration getDuration() {
    return getElapsed(processElapsed, processInstant, paused);
  }

  public void pause() {
    if (!paused) {
      synchronized (lock) {
        if (!paused) {
          paused = true;

          var now = Instant.now();
          speedElapsed = speedElapsed.plus(Duration.between(speedInstant, now));
          processElapsed = processElapsed.plus(Duration.between(processInstant, now));
        }
      }
    }
  }

  public void resume() {
    if (paused) {
      synchronized (lock) {
        if (paused) {
          paused = false;

          processInstant = Instant.now();
          speedInstant = processInstant;
        }
      }
    }
  }

  private static Duration getElapsed(Duration elapsed, Instant instant, boolean paused) {
    return paused ? elapsed : elapsed.plus(Duration.between(instant, Instant.now()));
  }

  private interface ProgressSpeed {
    double getSpeed();
    Duration getEta(long remainingProgress);

    static Duration getEta(long remainingProgress, long elapsedSeconds, long progress) {
      if (remainingProgress < Long.MAX_VALUE / elapsedSeconds) { // check on overflow
        return Duration.ofSeconds(elapsedSeconds * remainingProgress / progress);
      }

      double speed = (double) progress / elapsedSeconds;
      return speed != 0 ? Duration.ofSeconds((long) (remainingProgress / speed)) : null;
    }
  }

  private class AvgProgressSpeed implements ProgressSpeed {

    @Override
    public double getSpeed() {
      var delta = ProgressState.this.speedDelta.get();
      if (delta <= 0) {
        return 0;
      }

      var elapsed = ProgressState.getElapsed(
          ProgressState.this.processElapsed,
          ProgressState.this.processInstant,
          ProgressState.this.paused
      );

      long elapsedSeconds = elapsed.toSeconds();
      return elapsedSeconds != 0 ? (double) delta / elapsedSeconds : 0;
    }

    @Override
    public Duration getEta(long remainingProgress) {
      var delta = ProgressState.this.speedDelta.get();
      if (delta <= 0) {
        return null;
      }

      var elapsed = ProgressState.getElapsed(
          ProgressState.this.processElapsed,
          ProgressState.this.processInstant,
          ProgressState.this.paused
      );

      long elapsedSeconds = elapsed.toSeconds();
      return elapsedSeconds != 0
          ? ProgressSpeed.getEta(remainingProgress, elapsedSeconds, delta)
          : null;
    }
  }

  private static class CurrentProgressSpeed implements ProgressSpeed {
    private final long timeInSeconds;
    private final long progress;

    public CurrentProgressSpeed(long timeInSeconds, long progress) {
      assert timeInSeconds != 0;

      this.timeInSeconds = timeInSeconds;
      this.progress = progress;
    }

    @Override
    public double getSpeed() {
      return progress != 0 ? (double) progress / timeInSeconds : 0;
    }

    @Override
    public Duration getEta(long remainingProgress) {
      if (remainingProgress <= 0) {
        return Duration.ZERO;
      }

      return progress != 0
          ? ProgressSpeed.getEta(remainingProgress, timeInSeconds, progress)
          : null;
    }
  }
}
