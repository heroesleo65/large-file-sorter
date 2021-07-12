package org.example.sorter.chunks;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.example.context.DefaultApplicationContext;
import org.example.context.StringContext;
import org.example.io.MockOutputStream;
import org.example.io.OutputStreamFactory;
import org.example.utils.FileHelper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Log4j2
class UnsortedChunkTest {
  private static final List<String> UNSORTED_LINES = asList(
      "qwe", "asd", "цук", "wer", "sdf", "xcv"
  );

  @SuppressWarnings("unused")
  static Stream<Arguments> getSortAndSaveParametersProvider() {
    return Stream.of(
        Arguments.of(
            3, UNSORTED_LINES, asList("asd", "qwe", "цук")
        ),
        Arguments.of(
            6, UNSORTED_LINES, asList("asd", "qwe", "sdf", "wer", "xcv", "цук")
        ),
        Arguments.of(
            12, UNSORTED_LINES, asList("asd", "qwe", "sdf", "wer", "xcv", "цук")
        )
    );
  }

  @ParameterizedTest
  @MethodSource("getSortAndSaveParametersProvider")
  void sortAndSaveWithReflection(int chunkSize, List<String> lines, List<String> result)
      throws IOException {
    var actualOutputStream = new ByteArrayOutputStream();

    var outputStreamFactory = mock(OutputStreamFactory.class);
    var stringContext = mock(StringContext.class);

    when(outputStreamFactory.getOutputStream(any(File.class)))
        .thenReturn(new MockOutputStream(actualOutputStream));

    when(stringContext.hasSupportReflection())
        .thenReturn(true);
    when(stringContext.getCoder(anyString()))
        .thenReturn((byte) 1);

    var codes = initFullValueArrayStringContext(stringContext, lines);

    var context = new DefaultApplicationContext(outputStreamFactory, stringContext);

    var chunk = new UnsortedChunk(new File(""), chunkSize, context);

    for (var line : lines) {
      chunk.add(line);
    }
    chunk.sort();
    chunk.save();

    var actual = actualOutputStream.toByteArray();
    var expected = getBytesFromLines(result, 1, codes, line -> 1);

    assertThat(actual).containsExactly(expected);
    assertThat(chunk.getCurrentSize()).isZero();

    verify(stringContext, times(result.size())).getValueArray(anyString());
    verify(stringContext, never()).getValueArray(anyString(), anyInt(), any(), any());
  }

  @ParameterizedTest
  @MethodSource("getSortAndSaveParametersProvider")
  void sortAndSaveWithoutReflection(int chunkSize, List<String> lines, List<String> result)
      throws IOException {
    var actualOutputStream = new ByteArrayOutputStream();

    var outputStreamFactory = mock(OutputStreamFactory.class);
    var stringContext = mock(StringContext.class);

    when(outputStreamFactory.getOutputStream(any(File.class)))
        .thenReturn(new MockOutputStream(actualOutputStream));

    when(stringContext.hasSupportReflection())
        .thenReturn(false);
    when(stringContext.getCoder(anyString()))
        .thenReturn((byte) -1);

    var codes = initPartValueArrayStringContext(stringContext, lines);

    var context = new DefaultApplicationContext(outputStreamFactory, stringContext);

    var chunk = new UnsortedChunk(new File(""), chunkSize, context);

    for (var line : lines) {
      chunk.add(line);
    }
    chunk.sort();
    chunk.save();

    var actual = actualOutputStream.toByteArray();
    var expected = getBytesFromLines(result, -1, codes, line -> 2 * line.length());

    assertThat(actual).containsExactly(expected);
    assertThat(chunk.getCurrentSize()).isZero();

    verify(stringContext, times(result.size())).getValueArray(anyString());
    verify(stringContext, times(2 * result.size()))
        .getValueArray(anyString(), anyInt(), any(), any());
  }

  private static Map<String, byte[]> initFullValueArrayStringContext(
      StringContext stringContext, Collection<String> lines
  ) {
    var result = new HashMap<String, byte[]>(lines.size());
    int number = 0;
    for (var line : lines) {
      var bytes = new byte[] { (byte) number };
      result.put(line, bytes);
      when(stringContext.getValueArray(eq(line))).thenReturn(bytes);
      number++;
    }
    return result;
  }

  private static Map<String, byte[]> initPartValueArrayStringContext(
      StringContext stringContext, Collection<String> lines
  ) {
    when(stringContext.getValueArray(anyString(), not(eq(0)), any(), any()))
        .thenReturn(0);

    var result = new HashMap<String, byte[]>(lines.size());
    int number = 0;
    for (var line : lines) {
      int curNumber = number;
      result.put(line, new byte[] { (byte) number, 0 });
      when(stringContext.getValueArray(eq(line), eq(0), any(), any()))
          .thenAnswer(invocation -> {
            var bytes = (byte[]) invocation.getArguments()[3];
            bytes[0] = (byte) curNumber;
            bytes[1] = 0;
            return 1;
          });
      number++;
    }
    return result;
  }

  private static byte[] getBytesFromLines(
      Iterable<String> lines,
      int coder,
      Map<String, byte[]> codes,
      Function<String, Integer> lengthFun
  ) throws IOException {
    var result = new ByteArrayOutputStream();
    for (var line : lines) {
      result.write(coder);
      FileHelper.writeInt(result, lengthFun.apply(line));
      result.write(codes.get(line));
    }
    return result.toByteArray();
  }
}
