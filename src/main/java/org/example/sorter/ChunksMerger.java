package org.example.sorter;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ChunksMerger {

  private final CopyableOutputChunk outputChunk;
  private final Comparator<String> comparator;

  public void merge(InputChunk[] chunks) {
    if (chunks.length < 3) {
      if (chunks.length == 1) {
        outputChunk.copyAndSave(chunks[0]);
      } else {
        merge(chunks[0], chunks[1]);
      }
      return;
    }

    var priorityQueue = new PriorityQueue<QueueItem>(
        chunks.length, (a, b) -> comparator.compare(a.data, b.data)
    );

    for (int i = 0; i < chunks.length; i++) {
      var data = chunks[i].pop();
      if (data != null) {
        priorityQueue.add(new QueueItem(data, i));
      } else {
        chunks[i] = null; // for GC
      }
    }

    switch (priorityQueue.size()) {
      case 0:
        return;
      case 1: {
        var item = priorityQueue.poll();
        outputChunk.add(item.data);
        outputChunk.copyAndSave(chunks[item.index]);
        break;
      }
      case 2: {
        mergeOfTwoArrays(chunks, priorityQueue);
        break;
      }
      default: {
        do {
          var item = priorityQueue.poll();

          // objects can't be null because priorityQueue.size() > 3
          //noinspection ConstantConditions
          final var nextString = priorityQueue.peek().data;
          //noinspection ConstantConditions
          var data = item.data;

          outputChunk.add(data);
          data = outputChunk.copyWithSaveUtil(
              chunks[item.index], v -> comparator.compare(v, nextString) < 0
          );
          if (data != null) {
            item.data = data;
            priorityQueue.add(item);
          } else {
            chunks[item.index] = null; // for GC
            if (priorityQueue.size() == 2) {
              mergeOfTwoArrays(chunks, priorityQueue);
              return;
            }
          }
        } while (true);
      }
    }
  }

  public void merge(final InputChunk firstChunk, final InputChunk secondChunk) {
    var firstString = firstChunk.pop();
    var secondString = secondChunk.pop();

    if (firstString == null) {
      if (secondString != null) {
        outputChunk.add(secondString);
        outputChunk.copyAndSave(secondChunk);
      }
    } else if (secondString != null) {
      merge(firstChunk, firstString, secondChunk, secondString);
    } else {
      outputChunk.add(firstString);
      outputChunk.copyAndSave(firstChunk);
    }
  }

  private void mergeOfTwoArrays(InputChunk[] chunks, Queue<QueueItem> queue) {
    var first = queue.poll();
    var second = queue.poll();
    //noinspection ConstantConditions
    merge(chunks[first.index], first.data, chunks[second.index], second.data);
  }

  private void merge(
      final InputChunk firstChunk, final String firstStringOfFirstChunk,
      final InputChunk secondChunk, final String firstStringOfSecondChunk
  ) {
    var firstString = firstStringOfFirstChunk;
    var secondString = firstStringOfSecondChunk;

    if (comparator.compare(secondString, firstString) < 0) {
      secondString = copyUtilHasOrder(secondString, firstString, secondChunk, firstChunk);
      if (secondString == null) {
        return;
      }
    }

    do {
      firstString = copyUtilHasOrder(firstString, secondString, firstChunk, secondChunk);
      if (firstString == null) {
        return;
      }
      secondString = copyUtilHasOrder(secondString, firstString, secondChunk, firstChunk);
      if (secondString == null) {
        return;
      }
    } while (true);
  }

  private String copyUtilHasOrder(
      final String lsString, final String gtString,
      final InputChunk lsChunk, final InputChunk gtChunk
  ) {
    outputChunk.add(lsString);
    var nextString = outputChunk.copyWithSaveUtil(
        lsChunk, v -> comparator.compare(v, gtString) < 0
    );
    if (nextString == null) {
      outputChunk.add(gtString);
      outputChunk.copyAndSave(gtChunk);
    }
    return nextString;
  }

  @AllArgsConstructor
  private static class QueueItem {
    private String data;

    private final int index;
  }
}
