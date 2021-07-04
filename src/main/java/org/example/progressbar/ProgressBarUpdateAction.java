package org.example.progressbar;

public class ProgressBarUpdateAction implements Runnable {

  private final ProgressState progressState;
  private final ProgressBarRenderer renderer;
  private final ProgressBarConsumer consumer;
  private long last;

  public ProgressBarUpdateAction(
      ProgressState progressState, ProgressBarRenderer renderer, ProgressBarConsumer consumer
  ) {
    this.progressState = progressState;
    this.renderer = renderer;
    this.consumer = consumer;
    this.last = Long.MIN_VALUE;
  }

  @Override
  public void run() {
    if (!progressState.isPaused()) {
      long current = progressState.getCurrent();
      if (last != current) {
        last = current;
        var rendered = renderer.render(progressState, consumer.getMaxRenderedLength());
        consumer.accept(rendered);
      }
    }
  }
}
