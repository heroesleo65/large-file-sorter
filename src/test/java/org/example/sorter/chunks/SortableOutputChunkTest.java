package org.example.sorter.chunks;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.example.context.ApplicationContext;
import org.example.io.MockOutputStream;
import org.example.io.MockStringSerializer;
import org.example.sorter.chunks.ids.OutputChunkId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SortableOutputChunkTest {
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

    var outputChunkId = mock(OutputChunkId.class);
    var context = mock(ApplicationContext.class);
    var serializer = new MockStringSerializer();
    var outputStream = new MockOutputStream(actualOutputStream);

    when(outputChunkId.createOutputStream()).thenReturn(outputStream);

    var chunk = new SortableOutputChunk(
        outputChunkId, chunkSize, serializer, Comparator.naturalOrder(), context
    );
    for (var line : lines) {
      chunk.add(line);
    }
    chunk.save();

    serializer.verify(
        /* times = */ 1,
        outputStream,
        result.toArray(new String[chunkSize]),
        0,
        result.size()
    );
    assertThat(chunk.isEmpty()).isTrue();
  }
}
