package org.example.progressbar;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.ExtensionMethod;
import org.example.extensions.StringBuilderExtensions;
import org.example.utils.StringDisplayHelper;

@ExtensionMethod({StringBuilderExtensions.class})
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class DefaultProgressBarRenderer implements ProgressBarRenderer {

  private final ProgressBarStyle style;
  private final String unitName;
  private final long unitSize;
  private final boolean isSpeedShown;
  private final DecimalFormat speedFormat;
  private final ChronoUnit speedUnit;

  @Override
  public String render(ProgressState progress, int maxLength) {
    var now = Instant.now();
    var startInstant = progress.getStartInstant();
    var extraMessage = progress.getExtraMessage();
    var start = progress.getStart();
    var current = progress.getCurrent();
    var max = progress.getMax();
    if (max >= 0 && max < current) {
      max = current;
    }

    var elapsed = Duration.between(startInstant, now);

    var result = new StringBuilder(maxLength)
        .append(progress.getTaskName())
        .append(' ')
        .appendPercentage(current, max)
        .append(style.getLeftBracket());

    int curLength = StringDisplayHelper.trimDisplayLength(result, maxLength - 1);

    // length of progress should be at least 1
    int maxSuffixLength = Math.max(maxLength - curLength - 1, 0);

    var suffix = new StringBuilder(extraMessage.length() + 16);
    if (maxSuffixLength > 0) {
      suffix.append(style.getRightBracket())
          .append(' ')
          .appendRatio(current, max, unitSize, unitName)
          .append(" (")
          .appendFormatDuration(elapsed)
          .append(" / ")
          .appendEta(start, current, max, elapsed)
          .append(") ");

      if (isSpeedShown) {
        appendSpeed(suffix, start, current, elapsed);
      }

      suffix.append(extraMessage);

      curLength += StringDisplayHelper.trimDisplayLength(suffix, maxSuffixLength);
    }

    int length = maxLength - curLength;

    if (max < 0) {
      // case of indefinite progress bars
      int pos = (int)(current % length);
      result.appendRepeat(style.getSpace(), pos)
          .append(style.getBlock())
          .appendRepeat(style.getSpace(), length - pos - 1);
    } else {
      // case of definite progress bars
      int progressIntegralPart = progressIntegralPart(current, max, length);

      result.appendRepeat(style.getBlock(), progressIntegralPart);
      if (current < max) {
        int progressFractionalPart = progressFractionalPart(current, max, length);
        result.append(style.getFractionSymbols().charAt(progressFractionalPart))
            .appendRepeat(style.getSpace(), length - progressIntegralPart - 1);
      }
    }

    result.append(suffix);
    return result.toString();
  }

  // Number of full blocks
  private int progressIntegralPart(long current, long max, int length) {
    if (max < 0) {
      return 0;
    }
    if (current >= max) {
      return length;
    }
    return (int) (length * current / max);
  }

  private int progressFractionalPart(long current, long max, int length) {
    if (max < 0 || max <= current) {
      return 0;
    }

    // TODO: add comments for description
    return (int) (length * current % max * style.getFractionSymbols().length() / max);
  }

  private void appendSpeed(StringBuilder builder, long start, long current, Duration elapsed) {
    String suffix = "/s";
    double elapsedSeconds = elapsed.getSeconds();
    double elapsedInUnit = elapsedSeconds;
    if (speedUnit != null) {
      switch (speedUnit) {
        case MINUTES:
          suffix = "/min";
          elapsedInUnit /= 60;
          break;
        case HOURS:
          suffix = "/h";
          elapsedInUnit /= (60 * 60);
          break;
        case DAYS:
          suffix = "/d";
          elapsedInUnit /= (60 * 60 * 24);
          break;
      }
    }

    if (elapsedSeconds == 0) {
      builder.append('?');
    } else {
      double speed = (double) (current - start) / elapsedInUnit;
      builder.append(speedFormat.format(speed / unitSize));
    }
    builder.append(unitName).append(suffix);
  }
}
