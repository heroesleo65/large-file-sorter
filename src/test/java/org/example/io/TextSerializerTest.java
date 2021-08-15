package org.example.io;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TextSerializerTest {

  @SuppressWarnings("unused")
  static Stream<Arguments> getTextSerializerWriteParametersProvider() {
    return Stream.of(
        Arguments.of(
            UTF_8, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 4, 3
        ),
        Arguments.of(
            UTF_8, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 0, 7
        ),
        Arguments.of(
            UTF_8, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 3, 5
        ),
        Arguments.of(
            US_ASCII, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 0, 7
        ),
        Arguments.of(
            US_ASCII, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 3, 5
        ),
        Arguments.of(
            UTF_16LE, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 0, 7
        ),
        Arguments.of(
            UTF_16LE, new String[] {"foo", "bar", "qwe", "asd", "zxc", "kek", "lol"}, 3, 5
        )
    );
  }

  @ParameterizedTest
  @MethodSource("getTextSerializerWriteParametersProvider")
  void write(Charset charset, String[] data, int from, int to) throws Exception {
    var actualOutputStream = new ByteArrayOutputStream();
    var outputStream = new MockOutputStream(actualOutputStream);

    var textSerializer = new TextSerializer(charset);
    textSerializer.write(outputStream, data, from, to);

    var actual = actualOutputStream.toString(charset);

    String expected;
    if (to >= from) {
      expected = Stream.of(data)
          .skip(from)
          .limit(to - from)
          .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator()));
    } else {
      expected = "";
    }

    assertThat(actual).isEqualTo(expected);
  }
}
