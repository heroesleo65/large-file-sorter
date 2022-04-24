package org.example.progressbar;

import java.text.DecimalFormat;
import java.time.temporal.ChronoUnit;
import lombok.experimental.ExtensionMethod;
import org.example.extensions.StringBuilderExtensions;
import org.example.utils.StringDisplayHelper;

@ExtensionMethod({StringBuilderExtensions.class})
public class DefaultProgressBarRenderer implements ProgressBarRenderer {

  private final ProgressBarStyle style;
  private final String unitName;
  private final long unitSize;
  private final boolean isSpeedShown;
  private final DecimalFormat speedFormat;
  private final ChronoUnit speedUnit;

  protected DefaultProgressBarRenderer(
      ProgressBarStyle style,
      String unitName,
      long unitSize,
      boolean isSpeedShown,
      DecimalFormat speedFormat,
      ChronoUnit speedUnit
  ) {
    this.style = style;
    this.unitName = unitName;
    this.unitSize = unitSize;
    this.isSpeedShown = isSpeedShown;
    this.speedFormat = speedFormat;
    this.speedUnit = speedUnit != null ? speedUnit : ChronoUnit.SECONDS;
  }

  @Override
  public String render(ProgressState progress, int maxLength) {
    if (maxLength <= 0) {
      return "";
    }

    var duration = progress.getDuration();
    var extraMessage = progress.getExtraMessage();
    var current = progress.getCurrent();
    var max = progress.getMax();
    var eta = progress.getEta();
    var speed = progress.getSpeed();
    if (max >= 0 && max < current) {
      max = current;
    }

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
          .appendFormatDuration(duration)
          .append(" / ")
          .appendEta(eta)
          .append(") ");

      if (isSpeedShown) {
        appendSpeed(suffix, speed);
      }

      suffix.append(extraMessage);

      curLength += StringDisplayHelper.trimDisplayLength(suffix, maxSuffixLength);
    }

    int length = maxLength - curLength;

    if (max < 0) {
      // case of indefinite progress bars
      int pos = (int) (current % length);
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

    if (current < Integer.MAX_VALUE) { // check on overflow
      return (int) (length * current / max);
    } else {
      return (int) (length * ((double) current / max));
    }
  }

  private int progressFractionalPart(long current, long max, int length) {
    if (max < 0 || max <= current) {
      return 0;
    }

    if (current < Integer.MAX_VALUE && max < Integer.MAX_VALUE) { // check on overflow
      // TODO: add comments for description
      return (int) (length * current % max * style.getFractionSymbols().length() / max);
    } else {
      double p = length * ((double) current / max);
      double fraction = (p - Math.floor(p)) * style.getFractionSymbols().length();
      return (int) Math.floor(fraction);
    }
  }

  private void appendSpeed(StringBuilder builder, double speed) {
    String suffix = "/s";
    switch (speedUnit) {
      case MINUTES:
        suffix = "/min";
        speed /= 60;
        break;
      case HOURS:
        suffix = "/h";
        speed /= (60 * 60);
        break;
      case DAYS:
        suffix = "/d";
        speed /= (60 * 60 * 24);
        break;
      default:
        break;
    }

    if (speed == 0) {
      builder.append('?');
    } else {
      builder.append(speedFormat.format(speed / unitSize));
    }
    builder.append(unitName).append(suffix);
  }
}
