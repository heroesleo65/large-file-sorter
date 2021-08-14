package org.example.sorter.chunks.ids;

import java.io.IOException;
import java.io.OutputStream;

public interface OutputChunkId extends ChunkId {
  OutputStream createOutputStream() throws IOException;
  String getMessageOnFailSave();
}
