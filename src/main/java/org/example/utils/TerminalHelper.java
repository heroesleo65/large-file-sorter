package org.example.utils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.log4j.Log4j2;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

@Log4j2
public final class TerminalHelper {

  public static final char CARRIAGE_RETURN_CHAR = '\r';
  public static final char ESCAPE_CHAR = '\u001b';

  private static final int DEFAULT_TERMINAL_WIDTH = 80;

  private static volatile Terminal terminal;
  private static volatile Boolean cursorMovementSupport;
  private static final AtomicInteger terminalRefCounter = new AtomicInteger();

  private TerminalHelper() {
    throw new UnsupportedOperationException("TerminalHelper is utility");
  }

  public static void openTerminal() {
    if (terminalRefCounter.getAndIncrement() == 0) {
      if (terminal == null) {
        synchronized (TerminalHelper.class) {
          if (terminal == null) {
            try {
              terminal = TerminalBuilder.builder().dumb(true).build();
              cursorMovementSupport = calculateCursorMovementSupport(terminal);
            } catch (IOException ex) {
              log.error("Dumb terminal was not created", ex);
            }
          }
        }
      }
    }
  }

  public static void closeTerminal() {
    int counter = terminalRefCounter.decrementAndGet();
    if (counter == 0) {
      destroyTerminal();
    } else if (counter < 0) {
      throw new IllegalStateException("Too many close calls");
    }
  }

  public static void forceCloseTerminal() {
    destroyTerminal();
    terminalRefCounter.set(0);
  }

  public static int getTerminalWidth() {
    if (terminal != null) {
      int width = terminal.getWidth();
      return (width >= 10) ? width : DEFAULT_TERMINAL_WIDTH;
    }

    throw new IllegalStateException("Terminal was not created");
  }

  public static boolean hasCursorMovementSupport() {
    Boolean movementSupport = cursorMovementSupport;
    if (movementSupport != null) {
      return movementSupport;
    }
    if (terminal != null) {
      return calculateCursorMovementSupport(terminal);
    }

    throw new IllegalStateException("Terminal was not created");
  }

  public static StringBuilder appendMoveCursorUp(StringBuilder builder, int count) {
    return builder.append(ESCAPE_CHAR)
        .append('[')
        .append(count)
        .append('A')
        .append(CARRIAGE_RETURN_CHAR);
  }

  public static StringBuilder appendMoveCursorDown(StringBuilder builder, int count) {
    return builder.append(ESCAPE_CHAR)
        .append('[')
        .append(count)
        .append('B')
        .append(CARRIAGE_RETURN_CHAR);
  }

  private static boolean calculateCursorMovementSupport(Terminal terminal) {
    return terminal.getStringCapability(InfoCmp.Capability.cursor_up) != null &&
        terminal.getStringCapability(InfoCmp.Capability.cursor_down) != null;
  }

  private static void destroyTerminal() {
    if (terminal != null) {
      synchronized (TerminalHelper.class) {
        if (terminal != null) {
          try {
            terminal.close();
            terminal = null;
            cursorMovementSupport = null;
          } catch (Exception ex) {
            log.error("Terminal was not closed", ex);
          }
        }
      }
    }
  }
}
