package org.example.progressbar;

import org.example.tags.LargeTest;

public class CheckProgressBarOnDisplayTest {
  @LargeTest
  void progressBarFromFastToSlowSpeed() throws Exception {
    final long steps = 150;
    final long incrementStep = 100;

    try (var progressBarGroup = new ProgressBarGroup()) {
      var progressBar = progressBarGroup.createProgressBar(
          /* task = */ "From fast to slow speed...", /* initialMax = */ steps * incrementStep
      );

      for (long i = 0; i < steps; i++) {
        Thread.sleep(i * (long) Math.sqrt(i));
        progressBar.stepBy(incrementStep);
      }
    }
  }
}
