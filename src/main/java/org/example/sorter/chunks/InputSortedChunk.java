package org.example.sorter.chunks;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import org.example.context.ApplicationContext;
import org.example.utils.StreamHelper;

@Log4j2
public class InputSortedChunk extends AbstractChunk {
  private static final int DELETE_ON_EXIT_ATTRIBUTE = 0x01;
  private static final int LOADED_FILE_ATTRIBUTE = 0x02;

  private final int id;
  private final File inputFile;
  private long position;
  private byte attributes = DELETE_ON_EXIT_ATTRIBUTE;
  private final ApplicationContext context;

  public InputSortedChunk(int id, int chunkSize, ApplicationContext context) {
    super(chunkSize);
    this.id = id;
    this.inputFile = context.getFileSystemContext().getTemporaryFile(id);
    this.context = context;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public String pop() {
    var result = super.pop();
    if (result == null) {
      setLoadedFile();
      deleteTemporaryFile();
    }
    return result;
  }

  @Override
  public boolean load() {
    if (!hasAccessToFile()) {
      return false;
    }

    try (var file = context.getStreamFactory().getRandomAccessInputStream(inputFile)) {
      if (position >= file.length()) {
        return false;
      }
      file.seek(position);

      var values = new byte[0];
      var builder = new StringBuilder(0);
      for (int i = getCurrentCursor(); i < data.length; i++) {
        int coder = file.read();
        if (coder < 0) {
          break;
        }

        var len = StreamHelper.readInt(file);
        if (len < 0) {
          log.error("Unexpected end of file '{}'", inputFile);
          context.sendIOExceptionEvent(new EOFException());
          break;
        }

        // TODO: add comments for explanation
        if (context.getStringContext().hasSupportReflection() || values.length < len) {
          values = new byte[len];
        }
        file.read(values, 0, len);

        var line = context.getStringContext().createString(
            values, (byte) (coder & 0xFF), len, builder
        );
        uncheckedAdd(line);
      }

      position = file.getFilePointer();
      return true;
    } catch (FileNotFoundException ex) {
      fileNotFound();
    } catch (IOException ex) {
      log.error(() -> "Can't load file '" + inputFile + "'", ex);
      position = Long.MAX_VALUE;
      context.sendIOExceptionEvent(ex);
    }

    return false;
  }

  @Override
  public void save() {
    throw new UnsupportedOperationException("Save is not supported");
  }

  public void setDeleteOnExit(boolean value) {
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

    // We don't delete not own file
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

  private void deleteTemporaryFile() {
    if (isDeleteOnExit()) {
      if (!context.getFileSystemContext().delete(inputFile)) {
        log.error("Can't delete file '{}'", inputFile);
      }
    }
  }
}
