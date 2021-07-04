package org.example.extensions;

import java.time.Duration;

public final class StringBuilderExtensions {

  private StringBuilderExtensions() {
    throw new UnsupportedOperationException("StringBuilderExtensions is extension");
  }

  public static StringBuilder appendRepeat(StringBuilder builder, char c, int n) {
    if (n <= 0) {
      return builder;
    }

    builder.ensureCapacity(builder.capacity() + n);
    for (int i = 0; i < n; i++) {
      builder.append(c);
    }

    return builder;
  }

  public static StringBuilder appendPercentage(StringBuilder builder, long current, long max) {
    var result = max <= 0 ? "? " : String.valueOf(100 * current / max);
    return appendRepeat(builder, ' ', 3 - result.length())
        .append(result)
        .append('%');
  }

  public static StringBuilder appendEta(
      StringBuilder builder, long start, long current, long max, Duration elapsed
  ) {
    if (max <= 0 || current == start) {
      return builder.append('?');
    }
    return appendFormatDuration(
        builder, elapsed.dividedBy(current - start).multipliedBy(max - current)
    );
  }

  public static StringBuilder appendRatio(
      StringBuilder builder, long current, long max, long unitSize, String unitName
  ) {
    var m = max < 0 ? "?" : String.valueOf(max / unitSize);
    var c = String.valueOf(current / unitSize);

    return appendRepeat(builder, ' ', m.length() - c.length())
        .append(c)
        .append('/')
        .append(m)
        .append(unitName);
  }

  public static StringBuilder appendFormatDuration(StringBuilder builder, Duration d) {
    long seconds = d.getSeconds();

    builder.append(seconds / 3600).append(':');
    appendFormatTime(builder, (seconds % 3600) / 60).append(':');
    return appendFormatTime(builder, seconds % 60);
  }

  public static StringBuilder appendFormatTime(StringBuilder builder, long value) {
    if (value < 10) {
      builder.append('0');
    }
    return builder.append(value);
  }
}
