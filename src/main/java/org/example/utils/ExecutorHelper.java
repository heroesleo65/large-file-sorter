package org.example.utils;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class ExecutorHelper {

  private ExecutorHelper() {
    throw new UnsupportedOperationException("ExecutorHelper is utility");
  }

  public static boolean close(ExecutorService executorService) {
    executorService.shutdown();

    try {
      if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
        return executorService.awaitTermination(60, TimeUnit.SECONDS);
      }
      return true;
    } catch (InterruptedException ex) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }

    return false;
  }
}
