package org.example.sorter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum SortState {
  PARTITION_SORT("Partition sorting..."),
  MERGE("Merging to temporary files..."),
  SAVE_OUTPUT("Final merging and saving to output file...");

  @Getter
  private final String description;
}
