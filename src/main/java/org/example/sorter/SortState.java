package org.example.sorter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum SortState {
  PARTITION_SORT("Partition sorting..."),
  MERGE("Merging to temporary files..."),
  SAVE_OUTPUT("Final merging and saving to output file...");

  private final String description;
}
