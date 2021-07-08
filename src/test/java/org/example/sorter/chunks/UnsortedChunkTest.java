package org.example.sorter.chunks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.example.context.DefaultApplicationContext;
import org.example.io.MockOutputStream;
import org.example.io.OutputStreamFactory;
import org.example.utils.FileHelper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Log4j2
class UnsortedChunkTest {
  private static final List<String> UNSORTED_ASCII_LINES = Arrays.asList(
    "qwe", "asd", "zxc", "wer", "sdf", "xcv"
  );

  private static final List<String> SORTED_ASCII_LINES = Arrays.asList(
    "asd", "qwe", "sdf", "wer", "xcv", "zxc"
  );

  @ParameterizedTest
  @CsvSource({"true", "false"})
  void sortAndSave(boolean reflectionFeature) throws IOException {
    var actualOutputStream = new ByteArrayOutputStream();

    var outputStreamFactory = mock(OutputStreamFactory.class);
    when(outputStreamFactory.getOutputStream(any(File.class)))
        .thenReturn(new MockOutputStream(actualOutputStream));

    var context = new DefaultApplicationContext(outputStreamFactory, reflectionFeature);

    var chunk = new UnsortedChunk(new File(""), 12, context);

    for (var line : UNSORTED_ASCII_LINES) {
      chunk.add(line);
    }

    chunk.sort();
    chunk.save();

    var hasSupportReflection = context.hasSupportReflection();
    if (hasSupportReflection != reflectionFeature) {
      log.warn("Change status of reflection");
    }

    var expectedOutputStream = new ByteArrayOutputStream();
    addAsciiLines(expectedOutputStream, SORTED_ASCII_LINES, context.hasSupportReflection());

    var actual = actualOutputStream.toByteArray();
    var expected = expectedOutputStream.toByteArray();

    assertThat(actual).containsExactly(expected);
  }

  private void addAsciiLines(
      OutputStream stream, Collection<String> lines, boolean reflection
  ) throws IOException {
    for (var line : lines) {
      if (reflection) {
        addAsciiLineReflection(stream, line);
      } else {
        addAsciiLine(stream, line);
      }
    }
  }

  private void addAsciiLine(OutputStream stream, String line) throws IOException {
    var chars = line.toCharArray();

    stream.write(-1);
    FileHelper.writeInt(stream, 2 * chars.length);
    for (char symbol : chars) {
      stream.write((byte) ((symbol >>> 8) & 0xFF));
      stream.write((byte) (symbol & 0xFF));
    }
  }

  private void addAsciiLineReflection(OutputStream stream, String line) throws IOException {
    var chars = line.toCharArray();

    stream.write(0);
    FileHelper.writeInt(stream, chars.length);
    for (char symbol : chars) {
      stream.write((byte) symbol);
    }
  }
}
