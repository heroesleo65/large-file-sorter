package org.example.progressbar;

import java.util.function.Consumer;

public interface ProgressBarConsumer extends
    Consumer<String>, AutoCloseable, ListOfItemsListener<ProgressBarConsumer> {

  /**
   * Returns the maximum length allowed for the rendered form of a progress bar.
   */
  int getMaxRenderedLength();

  /**
   * Accepts a rendered form of a progress bar, e.g., prints to a specified stream.
   *
   * @param rendered Rendered form of a progress bar, a string
   */
  void accept(String rendered);

  void close();
}
