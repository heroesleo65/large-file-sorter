package org.example.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.example.base.DataSize;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class DataSizeHelperTest {

  @SuppressWarnings("unused")
  static Stream<Arguments> getParseParametersProvider() {
    return Stream.of(
        Arguments.of("13", DataSize.ofBytes(13)),
        Arguments.of(" 13  ", DataSize.ofBytes(13)),
        Arguments.of("  13b ", DataSize.ofBytes(13)),
        Arguments.of(" 343K  ", DataSize.ofKiloBytes(343)),
        Arguments.of(" 343k  ", DataSize.ofKiloBytes(343)),
        Arguments.of(" 13Kb  ", DataSize.ofKiloBytes(13)),
        Arguments.of(" 643M  ", DataSize.ofMegaBytes(643)),
        Arguments.of(" 13Mb  ", DataSize.ofMegaBytes(13)),
        Arguments.of(" 71G  ", DataSize.ofGigaBytes(71)),
        Arguments.of(" 05Gb  ", DataSize.ofGigaBytes(5)),
        Arguments.of(" 05gb  ", DataSize.ofGigaBytes(5))
    );
  }

  @ParameterizedTest
  @MethodSource("getParseParametersProvider")
  void parse(String text, DataSize expected) {
    var dataSize = DataSizeHelper.parse(text);
    assertThat(dataSize).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
          "", "  ", "K", "M", "Mb", "  Gb", "qwe", "  qwe", " +23b", "23Mbqwe", "23Mb qwe", "23 M",
          "9223372036854775808", "92233720368547758Gb"
      }
  )
  void parseException(String text) {
    assertThatThrownBy(() -> DataSizeHelper.parse(text)).isInstanceOf(Exception.class);
  }
}
