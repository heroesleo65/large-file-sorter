package org.example.sorter;

import java.util.PriorityQueue;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ChunksMerger {
  private final OutputChunk outputChunk;

  public void merge(InputChunk[] chunks) {
    var priorityQueue = new PriorityQueue<PriorityQueueItem>(chunks.length);

    for (int i = 0; i < chunks.length; i++) {
      var data = chunks[i].pop();
      if (data != null) {
        priorityQueue.add(new PriorityQueueItem(data, i));
      } else {
        chunks[i] = null; // for GC
      }
    }

    while (priorityQueue.size() > 1) {
      var item = priorityQueue.poll();

      // priorityQueue.peek() can't be null because priorityQueue.size() > 0
      //noinspection ConstantConditions
      var nextString = priorityQueue.peek().data;
      var data = item.data;
      do {
        outputChunk.add(data);
        data = chunks[item.index].pop();
      } while (data != null && data.compareTo(nextString) < 0);

      if (data != null) {
        item.data = data;
        priorityQueue.add(item);
      } else {
        chunks[item.index] = null; // for GC
      }
    }

    if (!priorityQueue.isEmpty()) {
      var item = priorityQueue.poll();

      var data = item.data;
      item.data = null; // for GC
      do {
        outputChunk.add(data);
        data = chunks[item.index].pop();
      } while (data != null);

      chunks[item.index] = null; // for GC
    }

    outputChunk.save();
  }

  @AllArgsConstructor
  private static class PriorityQueueItem implements Comparable<PriorityQueueItem> {
    private String data;

    private final int index;

    @Override
    public int compareTo(PriorityQueueItem other) {
      return data.compareTo(other.data);
    }
  }
}
