package org.example.progressbar;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import lombok.extern.log4j.Log4j2;
import org.example.utils.ExecutorHelper;
import org.jline.terminal.Terminal;

@Log4j2
public class ProgressBarGroup implements AutoCloseable {

  private final Collection<ProgressBar> progressBars;
  private final ProgressBarConsumerFactory progressBarConsumerFactory;
  private final ScheduledExecutorService executor;

  public ProgressBarGroup(Terminal terminal) {
    this.progressBars = new CopyOnWriteArrayList<>();
    this.progressBarConsumerFactory = new ProgressBarConsumerFactory(terminal);

    this.executor = new ScheduledThreadPoolExecutor(1, runnable -> {
      var thread = Executors.defaultThreadFactory().newThread(runnable);
      thread.setName("ProgressBar");
      thread.setDaemon(true);
      return thread;
    });
  }

  public ProgressBar createProgressBar(String task, long initialMax) {
    return createProgressBar(
        task,
        initialMax,
        /* updateIntervalMillis = */ 1000,
        System.err,
        ProgressBarStyle.COLORFUL_UNICODE_BLOCK,
        /* unitName = */ "",
        /* unitSize = */ 1,
        /* showSpeed = */ false,
        /* speedFormat = */ null,
        /* speedUnit = */ ChronoUnit.SECONDS,
        /* processed = */ 0L
    );
  }

  public ProgressBar createProgressBar(
      String task,
      long initialMax,
      int updateIntervalMillis,
      PrintStream os,
      ProgressBarStyle style,
      String unitName,
      long unitSize,
      boolean showSpeed,
      DecimalFormat speedFormat,
      ChronoUnit speedUnit,
      long processed
  ) {
    var render = new DefaultProgressBarRenderer(
        style, unitName, unitSize, showSpeed, speedFormat, speedUnit
    );
    var consumer = progressBarConsumerFactory.createConsoleConsumer(os);

    var progressBar = new ProgressBar(
        task, initialMax, updateIntervalMillis, processed, render, consumer, executor
    );
    progressBars.add(progressBar);
    return progressBar;
  }

  @Override
  public void close() {
    for (var progressBar : progressBars) {
      progressBar.close();
    }
    progressBars.clear();
    progressBarConsumerFactory.clear();

    if (!ExecutorHelper.close(executor)) {
      log.error("ScheduledExecutor in ProgressBarGroup did not terminate");
    }
  }
}
