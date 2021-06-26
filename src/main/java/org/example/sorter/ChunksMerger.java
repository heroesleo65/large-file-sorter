package org.example.sorter;

import java.util.PriorityQueue;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ChunksMerger {
  private final Chunk outputChunk;

  public void merge(Chunk[] chunks) {
    var priorityQueue = new PriorityQueue<PriorityQueueItem>(chunks.length);

    for (int i = 0; i < chunks.length; i++) {
      var data = chunks[i].pop();
      if (data != null) {
        priorityQueue.add(new PriorityQueueItem(data, i));
      } else {
        chunks[i] = null; // for GC
      }
    }

    while (!priorityQueue.isEmpty()) {
      var item = priorityQueue.poll();
      outputChunk.add(item.data);

      var data = chunks[item.index].pop();
      if (data != null) {
        item.data = data;
        priorityQueue.add(item);
      } else {
        chunks[item.index] = null; // for GC
      }
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
