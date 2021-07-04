package org.example.utils;

import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

@Log4j2
public final class TerminalHelper {

  public static final char CARRIAGE_RETURN_CHAR = '\r';
  public static final char ESCAPE_CHAR = '\u001b';

  private static final int DEFAULT_TERMINAL_WIDTH = 80;

  private TerminalHelper() {
    throw new UnsupportedOperationException("TerminalHelper is utility");
  }

  public static int getTerminalWidth(Terminal terminal) {
    int width = terminal.getWidth();
    return (width >= 10) ? width : DEFAULT_TERMINAL_WIDTH;
  }

  public static boolean hasCursorMovementSupport(Terminal terminal) {
    return terminal.getStringCapability(InfoCmp.Capability.cursor_up) != null &&
        terminal.getStringCapability(InfoCmp.Capability.cursor_down) != null;
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

  public static Terminal createTerminal() {
    try {
      // Defaulting to a dumb terminal when a supported terminal can not be correctly created
      // see https://github.com/jline/jline3/issues/291
      return TerminalBuilder.builder().dumb(true).build();
    } catch (IOException ex) {
      log.error("Dumb terminal was not created", ex);
    }

    return null;
  }
}
