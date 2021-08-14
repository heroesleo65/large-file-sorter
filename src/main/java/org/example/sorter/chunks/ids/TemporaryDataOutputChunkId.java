package org.example.sorter.chunks.ids;

import java.io.IOException;
import java.io.OutputStream;
import lombok.RequiredArgsConstructor;
import org.example.context.ApplicationContext;

@RequiredArgsConstructor
public class TemporaryDataOutputChunkId implements OutputChunkId {

  private final long id;
  private final ApplicationContext context;

  @Override
  public long getId() {
    return id;
  }

  @Override
  public OutputStream createOutputStream() throws IOException {
    return context.getOutputStream(id);
  }

  @Override
  public String getMessageOnFailSave() {
    var file = context.getFileSystemContext().getTemporaryFile(id);
    return "Can't save file to temporary file '" + file + "'";
  }
}
