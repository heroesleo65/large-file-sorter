package org.example.progressbar;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.utils.StringDisplayHelper;
import org.example.utils.TerminalHelper;
import org.jline.terminal.Terminal;

public class InteractiveConsoleProgressBarConsumer extends ConsoleProgressBarConsumer {
  private final AtomicInteger position = new AtomicInteger(1);
  private final AtomicBoolean firstTimeAccept = new AtomicBoolean(false);

  InteractiveConsoleProgressBarConsumer(PrintStream out, Terminal terminal) {
    super(out, terminal);
  }

  InteractiveConsoleProgressBarConsumer(PrintStream out, int predefinedWidth, Terminal terminal) {
    super(out, predefinedWidth, terminal);
  }

  @Override
  public void onAddedItemEvent(Class<? extends ProgressBarConsumer> item) {
    if (item == InteractiveConsoleProgressBarConsumer.class) {
      position.incrementAndGet();
    }
  }

  @Override
  public void accept(String str) {
    var builder = firstTimeAccept.compareAndSet(false, true)
        ? createFirstPrintBuilder(str)
        : createUsuallyPrintBuilder(str, position.get());

    out.println(builder);
  }

  private StringBuilder createFirstPrintBuilder(String str) {
    var builder = new StringBuilder(str.length() + 1)
        .append(TerminalHelper.CARRIAGE_RETURN_CHAR)
        .append(str);
    StringDisplayHelper.trimDisplayLength(builder, 1, getMaxRenderedLength());
    return builder;
  }

  private StringBuilder createUsuallyPrintBuilder(String str, int moveCursor) {
    var builder = new StringBuilder(str.length() + 14);

    TerminalHelper.appendMoveCursorUp(builder, moveCursor);

    var strOffset = builder.length();

    builder.append(str);
    StringDisplayHelper.trimDisplayLength(builder, strOffset, getMaxRenderedLength());

    TerminalHelper.appendMoveCursorDown(builder, moveCursor);

    return builder;
  }

  @Override
  public void close() {
    out.flush();
    onCloseEvent();
  }
}
