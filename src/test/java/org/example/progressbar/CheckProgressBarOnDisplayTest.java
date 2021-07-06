package org.example.progressbar;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CheckProgressBarOnDisplayTest {
  private static volatile Terminal terminal;

  @BeforeAll
  static void testInit() throws Exception {
    terminal = TerminalBuilder.builder().dumb(true).build();
  }

  @AfterAll
  static void testFinally() throws Exception {
    if (terminal != null) {
      terminal.close();
      terminal = null;
    }
  }

  @Test
  void ProgressBarFromFastToSlowSpeed() throws Exception {
    final long steps = 150;
    final long incrementStep = 100;

    try (var progressBarGroup = new ProgressBarGroup(terminal)) {
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
