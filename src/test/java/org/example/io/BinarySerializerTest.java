package org.example.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.stream.Stream;
import org.example.context.StringContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BinarySerializerTest {

  @SuppressWarnings("unused")
  static Stream<Arguments> getBinarySerializerWriteParametersProvider() {
    return Stream.of(
        Arguments.of(
            4, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 4, 3
        ),
        Arguments.of(
            4, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 0, 7
        ),
        Arguments.of(
            4, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 3, 5
        ),
        Arguments.of(
            8, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 4, 3
        ),
        Arguments.of(
            8, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 0, 7
        ),
        Arguments.of(
            8, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 3, 5
        ),
        Arguments.of(
            16, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 4, 3
        ),
        Arguments.of(
            16, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 0, 7
        ),
        Arguments.of(
            16, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 3, 5
        )
    );
  }

  @ParameterizedTest
  @MethodSource("getBinarySerializerWriteParametersProvider")
  void writeWithReflection(int bufferSize, String[] data, int from, int to) throws Exception {
    final byte coder = 1;

    var actualOutputStream = new ByteArrayOutputStream();
    var outputStream = new MockOutputStream(actualOutputStream);
    var stringContext = mock(StringContext.class);

    when(stringContext.hasSupportReflection()).thenReturn(true);
    when(stringContext.getCoder(anyString())).thenReturn(coder);

    for (int i = 0; i < data.length; i++) {
      when(stringContext.getValueArray(eq(data[i]))).thenReturn(
          (i & 1) == 0 ? new byte[] { 1, 2, 3, (byte) i } : new byte[] { 4, 5, (byte) i }
      );
    }

    var binarySerializer = new BinarySerializer(bufferSize, stringContext);
    binarySerializer.write(outputStream, data, from, to);

    var actual = actualOutputStream.toByteArray();

    var expected = new ByteArrayOutputStream(actual.length);
    for (int i = from; i < to; i++) {
      expected.write(
          (i % 2) == 0
              ? new byte[]{coder, 4, 1, 2, 3, (byte) i}
              : new byte[]{coder, 3, 4, 5, (byte) i}
      );
    }

    assertThat(actual).isEqualTo(expected.toByteArray());
  }


  @ParameterizedTest
  @MethodSource("getBinarySerializerWriteParametersProvider")
  void writeWithoutReflection(int bufferSize, String[] data, int from, int to) throws Exception {
    final byte coder = -1;

    var actualOutputStream = new ByteArrayOutputStream();
    var outputStream = new MockOutputStream(actualOutputStream);
    var stringContext = mock(StringContext.class);

    when(stringContext.hasSupportReflection()).thenReturn(false);
    when(stringContext.getCoder(anyString())).thenReturn(coder);
    when(stringContext.getValueArray(anyString())).thenReturn(null);

    for (int i = 0; i < data.length; i++) {
      final int curNumber = i;
      when(stringContext.getValueArray(eq(data[i]), anyInt(), any(), any())).thenAnswer(
          invocation -> {
            var actual = (curNumber & 1) == 0
                ? new byte[] { 1, 2, 3, 4, 5, (byte) curNumber }
                : new byte[] { 6, 7, 8, (byte) curNumber };

            var offset = invocation.getArgument(1, Integer.class);
            var maxLen = invocation.getArgument(2, char[].class).length;
            var bytes = invocation.getArgument(3, byte[].class);

            int count = 0;
            for (int j = offset; count < maxLen && 2 * j < actual.length; count++, j++) {
              bytes[2 * count] = actual[2 * j];
              bytes[2 * count + 1] = actual[2 * j + 1];
            }
            return count;
          }
      );
    }

    var binarySerializer = new BinarySerializer(bufferSize, stringContext);
    binarySerializer.write(outputStream, data, from, to);

    var actual = actualOutputStream.toByteArray();

    var expected = new ByteArrayOutputStream(actual.length);
    for (int i = from; i < to; i++) {
      expected.write(
          (i % 2) == 0
              ? new byte[]{coder, (byte) (2 * data[i].length()), 1, 2, 3, 4, 5, (byte) i}
              : new byte[]{coder, (byte) (2 * data[i].length()), 6, 7, 8, (byte) i}
      );
    }

    assertThat(actual).isEqualTo(expected.toByteArray());
  }
}
