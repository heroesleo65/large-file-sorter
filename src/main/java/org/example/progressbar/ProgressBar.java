package org.example.progressbar;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ProgressBar implements AutoCloseable {
  private final ProgressState progressState;
  private final ProgressBarConsumer progressConsumer;
  private final ProgressBarUpdateAction updateAction;
  private final ScheduledFuture<?> scheduledTask;

  ProgressBar(
      String task,
      long initialMax,
      int updateIntervalMillis,
      long processed,
      Duration elapsed,
      ProgressBarRenderer renderer,
      ProgressBarConsumer consumer,
      ScheduledExecutorService scheduledExecutorService
  ) {
    this.progressState = new ProgressState(task, initialMax, processed, elapsed);
    this.progressConsumer = consumer;

    this.updateAction = new ProgressBarUpdateAction(progressState, renderer, progressConsumer);
    this.scheduledTask = scheduledExecutorService.scheduleWithFixedDelay(
        updateAction, 0, updateIntervalMillis, TimeUnit.MILLISECONDS
    );
  }

  @Override
  public void close() {
    scheduledTask.cancel(false);
    updateAction.run();
    progressConsumer.close();
  }

  /**
   * Advances this progress bar by a specific amount.
   * @param n Step size
   */
  public ProgressBar stepBy(long n) {
    progressState.stepBy(n);
    return this;
  }

  /**
   * Advances this progress bar to the specific progress value.
   * @param n New progress value
   */
  public ProgressBar stepTo(long n) {
    progressState.stepTo(n);
    return this;
  }

  /**
   * Advances this progress bar by one step.
   */
  public ProgressBar step() {
    progressState.stepBy(1);
    return this;
  }

  /**
   * Gives a hint to the maximum value of the progress bar.
   * @param n Hint of the maximum value. A value of -1 indicates that the progress bar is indefinite.
   */
  public ProgressBar maxHint(long n) {
    progressState.setMaxHint(n);
    return this;
  }

  /**
   * Pauses this current progress.
   */
  public ProgressBar pause() {
    progressState.pause();
    return this;
  }

  /**
   * Resumes this current progress.
   */
  public ProgressBar resume() {
    progressState.resume();
    return this;
  }

  /**
   * Sets the extra message at the end of the progress bar.
   * @param msg New message
   */
  public ProgressBar setExtraMessage(String msg) {
    progressState.setExtraMessage(msg);
    return this;
  }

  /**
   * Returns the current progress.
   */
  public long getCurrent() {
    return progressState.getCurrent();
  }

  /**
   * Returns the maximum value of this progress bar.
   */
  public long getMax() {
    return progressState.getMax();
  }

  /**
   * Returns the name of this task.
   */
  public String getTaskName() {
    return progressState.getTaskName();
  }

  /**
   * Returns the extra message at the end of the progress bar.
   */
  public String getExtraMessage() {
    return progressState.getExtraMessage();
  }
}
