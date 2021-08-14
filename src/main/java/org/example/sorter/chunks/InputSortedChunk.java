package org.example.sorter.chunks;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.context.ApplicationContext;
import org.example.io.RandomAccessInputStream;
import org.example.io.StringDeserializer;

@Log4j2
public class InputSortedChunk extends AbstractInputChunk {
  private static final int DELETE_ON_EXIT_ATTRIBUTE = 0x01;
  private static final int LOADED_FILE_ATTRIBUTE = 0x02;

  private final long id;
  private final File inputFile;
  private final StringDeserializer deserializer;
  private final ApplicationContext context;

  private long position;
  private byte attributes = DELETE_ON_EXIT_ATTRIBUTE;

  public InputSortedChunk(
      long id, int chunkSize, StringDeserializer deserializer, ApplicationContext context
  ) {
    super(chunkSize);
    this.id = id;
    this.inputFile = context.getFileSystemContext().getTemporaryFile(id);
    this.deserializer = deserializer;
    this.context = context;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public String pop() {
    var result = super.pop();
    if (result == null) {
      setLoadedFile();
      freeResources();
    }
    return result;
  }

  @Override
  protected void freeResources() {
    if (isDeleteOnExit()) {
      if (!context.getFileSystemContext().delete(inputFile)) {
        log.error("Can't delete file '{}'", inputFile);
      }
    }
    super.freeResources();
  }

  @Override
  public boolean load() {
    try {
      return load(inputStream -> size = deserializer.read(inputStream, data, size));
    } catch (IOException ex) {
      log.error("Unexpected exception", ex);
      context.sendSignal(ex);
      return false;
    }
  }

  protected boolean copyData(int bufferSize, final Copier copier) throws IOException {
    return load(inputStream -> {
      final byte[] buffer = new byte[bufferSize];
      int count;
      while ((count = inputStream.read(buffer, 0, bufferSize)) > 0) {
        try {
          copier.copy(buffer, count);
        } catch (IOException ex) {
          throw new IOExceptionWrapper(ex);
        }
      }
    });
  }

  private boolean load(Reader reader) throws IOException {
    if (!hasAccessToFile()) {
      return false;
    }

    try (var file = context.getStreamFactory().getRandomAccessInputStream(inputFile)) {
      if (position >= file.length()) {
        return false;
      }
      file.seek(position);

      reader.read(file);

      position = file.getFilePointer();
      return true;
    } catch (IOExceptionWrapper ex) {
      throw ex.getException();
    } catch (FileNotFoundException ex) {
      fileNotFound();
    } catch (IOException ex) {
      if (ex instanceof EOFException) {
        log.error("Unexpected end of file '{}'", inputFile);
      } else {
        log.error(() -> "Can't load file '" + inputFile + "'", ex);
      }

      position = Long.MAX_VALUE;
      context.sendSignal(ex);
    }

    return false;
  }

  private void setDeleteOnExit(boolean value) {
    if (value) {
      attributes |= DELETE_ON_EXIT_ATTRIBUTE;
    } else {
      attributes &= ~DELETE_ON_EXIT_ATTRIBUTE;
    }
  }

  private boolean isDeleteOnExit() {
    return (attributes & DELETE_ON_EXIT_ATTRIBUTE) != 0;
  }

  private boolean isLoadedFile() {
    return (attributes & LOADED_FILE_ATTRIBUTE) != 0;
  }

  private void setLoadedFile() {
    attributes |= LOADED_FILE_ATTRIBUTE;
  }

  private void fileNotFound() {
    log.error("File '{}' was removed", inputFile);
    attributes = LOADED_FILE_ATTRIBUTE;
  }

  private boolean hasAccessToFile() {
    if (isLoadedFile()) {
      return false;
    }

    if (!context.getFileSystemContext().isFile(inputFile)) {
      if (context.getFileSystemContext().exists(inputFile)) {
        log.error("File for chunks '{}' is not file", inputFile);
        setLoadedFile();
      } else {
        fileNotFound();
      }
      return false;
    }

    if (!context.getFileSystemContext().canRead(inputFile)) {
      if (context.getFileSystemContext().exists(inputFile)) {
        log.error("User don't has read access to file '{}'", inputFile);
        setLoadedFile();
      } else {
        fileNotFound();
      }
      return false;
    }

    return true;
  }

  @FunctionalInterface
  public interface Reader {
    void read(RandomAccessInputStream inputStream) throws IOException;
  }

  @FunctionalInterface
  public interface Copier {
    void copy(byte[] bytes, int len) throws IOException;
  }

  @Getter
  @RequiredArgsConstructor
  private static class IOExceptionWrapper extends IOException {
    private final IOException exception;
  }
}
