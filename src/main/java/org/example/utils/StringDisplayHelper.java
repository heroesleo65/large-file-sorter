package org.example.utils;

import static org.jline.utils.WCWidth.wcwidth;

public final class StringDisplayHelper {

  private StringDisplayHelper() {
    throw new UnsupportedOperationException("StringDisplayHelper is utility");
  }

  /**
   * Returns the display width of a Unicode character on terminal.
   */
  public static int getCharDisplayLength(char c) {
    return Math.max(wcwidth(c), 0);
  }

  public static int trimDisplayLength(StringBuilder builder, int maxDisplayLength) {
    return trimDisplayLength(builder, 0, maxDisplayLength);
  }

  public static int trimDisplayLength(StringBuilder builder, int offset, int maxDisplayLength) {
    if (maxDisplayLength <= 0) {
      return 0;
    }

    int totalLength = 0;
    for (int i = offset; i < builder.length(); i++) {
      var length = getCharDisplayLength(builder.charAt(i));
      if (totalLength + length > maxDisplayLength) {
        builder.setLength(i);
        return totalLength;
      }
      totalLength += length;
    }
    return totalLength;
  }
}
