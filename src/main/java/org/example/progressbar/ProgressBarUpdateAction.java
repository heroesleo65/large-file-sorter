package org.example.progressbar;

import java.util.concurrent.atomic.AtomicLong;

public class ProgressBarUpdateAction implements Runnable {

  private final ProgressState progressState;
  private final ProgressBarRenderer renderer;
  private final ProgressBarConsumer consumer;
  private final AtomicLong last;

  public ProgressBarUpdateAction(
      ProgressState progressState, ProgressBarRenderer renderer, ProgressBarConsumer consumer
  ) {
    this.progressState = progressState;
    this.renderer = renderer;
    this.consumer = consumer;
    this.last = new AtomicLong(Long.MIN_VALUE);
  }

  @Override
  public void run() {
    if (!progressState.isPaused()) {
      long current = progressState.getCurrent();
      if (last.getAndSet(current) != current) {
        var rendered = renderer.render(progressState, consumer.getMaxRenderedLength());
        consumer.accept(rendered);
      }
    }
  }
}
