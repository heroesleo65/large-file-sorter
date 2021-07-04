package org.example.progressbar;

public interface ProgressBarRenderer {
  String render(ProgressState progress, int maxLength);
}
