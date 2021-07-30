package org.example.sorter.chunks;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.BiPredicate;
import lombok.extern.log4j.Log4j2;
import org.example.context.ApplicationContext;

@Log4j2
public class InputSortedChunk extends AbstractInputChunk {
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
    if (!hasAccessToFile()) {
      return false;
    }

    try (var file = context.getStreamFactory().getRandomAccessInputStream(inputFile)) {
      if (position >= file.length()) {
        return false;
      }
      file.seek(position);

      // MetaData:
      // first byte - coder
      // second byte - first decoded byte from len of string as 128 Base varint
      var metaData = new byte[2];

      var values = new byte[0];
      var builder = new StringBuilder(0);
      while (size < data.length) {
        int count = file.read(metaData, 0, 2);
        if (count <= 0) {
          break;
        }
        if (count < 2) {
          throw new EOFException();
        }

        int len = file.readVarint32(metaData[1]);
        if (len < 0) {
          throw new IOException("Negative length was loaded");
        }

        // TODO: add comments for explanation
        if (context.getStringContext().hasSupportReflection() || values.length < len) {
          values = new byte[len];
        }
        if (file.read(values, 0, len) != len) {
          throw new EOFException();
        }

        data[size++] = context.getStringContext().createString(values, metaData[0], len, builder);
      }

      position = file.getFilePointer();
      return true;
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

  protected boolean loadData(int bufferSize, BiPredicate<byte[], Integer> action) {
    if (!hasAccessToFile()) {
      return false;
    }

    try (var file = context.getStreamFactory().getRandomAccessInputStream(inputFile)) {
      if (position >= file.length()) {
        return false;
      }
      file.seek(position);

      final byte[] buffer = new byte[bufferSize];
      int count;
      while ((count = file.read(buffer, 0, bufferSize)) > 0) {
        if (!action.test(buffer, count)) {
          return false;
        }
      }

      position = file.getFilePointer();
      return true;
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
}
