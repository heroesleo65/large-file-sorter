package org.example.progressbar;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.log4j.Log4j2;
import org.example.utils.TerminalHelper;

@Log4j2
public class ProgressBarConsumerFactory {

  private final Collection<ProgressBarConsumer> consumers;

  public ProgressBarConsumerFactory() {
    this.consumers = new CopyOnWriteArrayList<>();
  }

  public ProgressBarConsumer createConsoleConsumer(int predefinedWidth) {
    var os = new PrintStream(new FileOutputStream(FileDescriptor.err));
    return createConsoleConsumer(os, predefinedWidth);
  }

  public ProgressBarConsumer createConsoleConsumer(PrintStream os) {
    return createConsoleConsumer(os, -1);
  }

  public ProgressBarConsumer createConsoleConsumer(PrintStream os, int predefinedWidth) {
    TerminalHelper.openTerminal();

    var progressBarConsumer = TerminalHelper.hasCursorMovementSupport()
        ? new InteractiveConsoleProgressBarConsumer(os, predefinedWidth)
        : new ConsoleProgressBarConsumer(os, predefinedWidth);
    if (add(progressBarConsumer)) {
      return progressBarConsumer;
    }

    return null;
  }

  public void clear() {
    consumers.clear();
  }

  private boolean add(ProgressBarConsumer item) {
    if (consumers.add(item)) {
      try {
        item.registerCloseEvent(this::remove);
      } catch (Exception ex) {
        log.error("Unexpected exception in registering close event on ProgressBarConsumer", ex);
        remove(item);
        return false;
      }

      var itemClass = item.getClass();
      for (var consumer : consumers) {
        if (consumer != item) {
          try {
            consumer.onAddedItemEvent(itemClass);
          } catch (Exception ex) {
            log.error("Unexpected exception on adding ProgressBarConsumer event", ex);
          }
        }
      }

      return true;
    }

    return false;
  }

  private void remove(ProgressBarConsumer item) {
    consumers.remove(item);
    TerminalHelper.closeTerminal();
  }
}
