package org.example.progressbar;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum ProgressBarStyle {
  COLORFUL_UNICODE_BLOCK("\r", "\u001b[33m│", "│\u001b[0m", '█', ' ', " ▏▎▍▌▋▊▉"),

  /** Use Unicode block characters to draw the progress bar. */
  UNICODE_BLOCK("\r", "│", "│", '█', ' ', " ▏▎▍▌▋▊▉"),

  /** Use only ASCII characters to draw the progress bar. */
  ASCII("\r", "[", "]", '=', ' ', ">");

  private final String refreshPrompt;
  private final String leftBracket;
  private final String rightBracket;
  private final char block;
  private final char space;
  private final String fractionSymbols;
}
