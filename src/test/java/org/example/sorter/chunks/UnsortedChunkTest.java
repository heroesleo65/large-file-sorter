package org.example.sorter.chunks;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.example.context.DefaultApplicationContext;
import org.example.io.MockOutputStream;
import org.example.io.OutputStreamFactory;
import org.example.utils.FileHelper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Log4j2
class UnsortedChunkTest {
  private static final List<String> UNSORTED_ASCII_LINES = asList(
      "qwe", "asd", "zxc", "wer", "sdf", "xcv"
  );

  private static final List<String> UNSORTED_UTF16_LINES = asList(
      "ячс", "йцу", "фыв", "цук", "ыва", "чсм"
  );

  @SuppressWarnings("unused")
  static Stream<Arguments> getSortAndSaveParametersProvider() {
    return Stream.of(
        Arguments.of(
            true, 3, true, UNSORTED_ASCII_LINES, asList("asd", "qwe", "zxc")
        ),
        Arguments.of(
            false, 3, true, UNSORTED_ASCII_LINES, asList("asd", "qwe", "zxc")
        ),
        Arguments.of(
            true, 6, true, UNSORTED_ASCII_LINES, asList("asd", "qwe", "sdf", "wer", "xcv", "zxc")
        ),
        Arguments.of(
            false, 6, true, UNSORTED_ASCII_LINES, asList("asd", "qwe", "sdf", "wer", "xcv", "zxc")
        ),
        Arguments.of(
            true, 12, true, UNSORTED_ASCII_LINES, asList("asd", "qwe", "sdf", "wer", "xcv", "zxc")
        ),
        Arguments.of(
            false, 12, true, UNSORTED_ASCII_LINES, asList("asd", "qwe", "sdf", "wer", "xcv", "zxc")
        ),
        Arguments.of(
            true, 3, false, UNSORTED_UTF16_LINES, asList("йцу", "фыв", "ячс")
        ),
        Arguments.of(
            false, 3, false, UNSORTED_UTF16_LINES, asList("йцу", "фыв", "ячс")
        ),
        Arguments.of(
            true, 6, false, UNSORTED_UTF16_LINES, asList("йцу", "фыв", "цук", "чсм", "ыва", "ячс")
        ),
        Arguments.of(
            false, 6, false, UNSORTED_UTF16_LINES, asList("йцу", "фыв", "цук", "чсм", "ыва", "ячс")
        ),
        Arguments.of(
            true, 12, false, UNSORTED_UTF16_LINES, asList("йцу", "фыв", "цук", "чсм", "ыва", "ячс")
        ),
        Arguments.of(
            false, 12, false, UNSORTED_UTF16_LINES, asList("йцу", "фыв", "цук", "чсм", "ыва", "ячс")
        )
    );
  }

  @ParameterizedTest
  @MethodSource("getSortAndSaveParametersProvider")
  void sortAndSave(
      boolean reflectionFeature,
      int chunkSize,
      boolean isAsciiSymbols,
      List<String> lines,
      List<String> result
  ) throws IOException {
    var actualOutputStream = new ByteArrayOutputStream();

    var outputStreamFactory = mock(OutputStreamFactory.class);
    when(outputStreamFactory.getOutputStream(any(File.class)))
        .thenReturn(new MockOutputStream(actualOutputStream));

    var context = new DefaultApplicationContext(outputStreamFactory, reflectionFeature);

    var chunk = new UnsortedChunk(new File(""), chunkSize, context);

    for (var line : lines) {
      chunk.add(line);
    }

    chunk.sort();
    chunk.save();

    var hasSupportReflection = context.getStringContext().hasSupportReflection();
    if (hasSupportReflection != reflectionFeature) {
      log.warn("Change status of reflection");
    }

    var actual = actualOutputStream.toByteArray();
    var expected = isAsciiSymbols
        ? getBytesFromAsciiLines(result, hasSupportReflection)
        : getBytesFromUtf16Lines(result, hasSupportReflection);

    assertThat(actual).containsExactly(expected);
  }

  private static byte[] getBytesFromUtf16Lines(Iterable<String> lines, boolean reflection)
      throws IOException {
    int coder = reflection ? 1 : -1;

    var outputStream = new ByteArrayOutputStream();
    for (var line : lines) {
      addLine(outputStream, line, coder);
    }
    return outputStream.toByteArray();
  }

  private static byte[] getBytesFromAsciiLines(Iterable<String> lines, boolean reflection)
      throws IOException {

    var outputStream = new ByteArrayOutputStream();
    if (reflection) {
      for (var line : lines) {
        addAsciiLine(outputStream, line);
      }
    } else {
      for (var line : lines) {
        addLine(outputStream, line);
      }
    }

    return outputStream.toByteArray();
  }

  private static void addLine(OutputStream stream, String line, int coder) throws IOException {
    var chars = line.toCharArray();

    stream.write(coder);
    FileHelper.writeInt(stream, 2 * chars.length);
    for (char symbol : chars) {
      stream.write((byte) ((symbol >>> 8) & 0xFF));
      stream.write((byte) (symbol & 0xFF));
    }
  }

  private static void addLine(OutputStream stream, String line) throws IOException {
    addLine(stream, line, /* coder = */ - 1);
  }

  private static void addAsciiLine(OutputStream stream, String line) throws IOException {
    var chars = line.toCharArray();

    stream.write(0);
    FileHelper.writeInt(stream, chars.length);
    for (char symbol : chars) {
      stream.write((byte) symbol);
    }
  }
}
