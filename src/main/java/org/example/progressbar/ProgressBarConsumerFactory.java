package org.example.progressbar;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.log4j.Log4j2;
import org.example.utils.TerminalHelper;
import org.jline.terminal.Terminal;

@Log4j2
public class ProgressBarConsumerFactory {

  private final Terminal terminal;
  private final Collection<ProgressBarConsumer> consumers;

  public ProgressBarConsumerFactory(Terminal terminal) {
    this.terminal = terminal;
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
    var progressBarConsumer = TerminalHelper.hasCursorMovementSupport(terminal)
        ? new InteractiveConsoleProgressBarConsumer(os, predefinedWidth, terminal)
        : new ConsoleProgressBarConsumer(os, predefinedWidth, terminal);
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
        item.registerCloseEvent(consumers::remove);
      } catch (Exception ex) {
        log.error("Unexpected exception in registering close event on ProgressBarConsumer", ex);
        consumers.remove(item);
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
}
