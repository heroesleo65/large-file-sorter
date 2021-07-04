package org.example.progressbar;

import java.io.PrintStream;
import java.util.function.Consumer;
import org.example.utils.StringDisplayHelper;
import org.example.utils.TerminalHelper;
import org.jline.terminal.Terminal;

public class ConsoleProgressBarConsumer implements ProgressBarConsumer {
  private static final int CONSOLE_RIGHT_MARGIN = 1;

  protected final PrintStream out;
  private final int maxRenderedLength;
  private final Terminal terminal;

  private volatile Consumer<ProgressBarConsumer> closeEvent;

  ConsoleProgressBarConsumer(PrintStream out, Terminal terminal) {
    this(out, -1, terminal);
  }

  ConsoleProgressBarConsumer(PrintStream out, int maxRenderedLength, Terminal terminal) {
    this.out = out;
    this.maxRenderedLength = maxRenderedLength;
    this.terminal = terminal;
  }

  @Override
  public void registerCloseEvent(Consumer<ProgressBarConsumer> closeEvent) {
    this.closeEvent = closeEvent;
  }

  @Override
  public int getMaxRenderedLength() {
    return maxRenderedLength <= 0
        ? TerminalHelper.getTerminalWidth(terminal) - CONSOLE_RIGHT_MARGIN
        : maxRenderedLength;
  }

  @Override
  public void accept(String str) {
    var builder = new StringBuilder(str.length() + 1)
        .append(TerminalHelper.CARRIAGE_RETURN_CHAR)
        .append(str);
    StringDisplayHelper.trimDisplayLength(builder, 1, getMaxRenderedLength());
    out.print(builder);
  }

  @Override
  public void close() {
    out.println();
    out.flush();

    onCloseEvent();
  }

  protected void onCloseEvent() {
    var event = closeEvent;
    if (event != null) {
      event.accept(this);
    }
  }
}
