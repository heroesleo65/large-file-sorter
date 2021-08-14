package org.example.sorter.chunks.ids;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import lombok.RequiredArgsConstructor;
import org.example.context.ApplicationContext;

@RequiredArgsConstructor
public class FileOutputChunkId implements OutputChunkId {

  private final File file;
  private final ApplicationContext context;

  @Override
  public long getId() {
    return 0;
  }

  @Override
  public OutputStream createOutputStream() throws IOException {
    return context.getStreamFactory().getOutputStream(file);
  }

  @Override
  public String getMessageOnFailSave() {
    return "Can't save data in file '" + file + "'";
  }
}
