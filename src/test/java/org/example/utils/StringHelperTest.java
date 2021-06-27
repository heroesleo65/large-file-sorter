package org.example.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StringHelperTest {
  @SuppressWarnings("unused")
  static Stream<Arguments> getRadixSortParametersProvider() {
    return Stream.of(
        Arguments.of(
            new String[0], 0, 0, new String[0]
        ),
        Arguments.of(
            new String[] { "aslk" }, 0, 1, new String[] { "aslk" }
        ),
        Arguments.of(
            new String[] { "aslk", "ablk" }, 0, 1, new String[] { "aslk", "ablk" }
        ),
        Arguments.of(
            new String[] { "aslk", "ablk" }, 0, 2, new String[] { "ablk", "aslk" }
        ),
        Arguments.of(
            new String[] { "aslk", "ablk", "v", "tr", "variant", "foo", "bar", "foobar" },
            0,
            4,
            new String[] { "ablk", "aslk", "tr", "v", "variant", "foo", "bar", "foobar" }
        ),
        Arguments.of(
            new String[] { "aslk", "ablk", "v", "tr", "variant", "foo", "bar", "foobar" },
            0,
            8,
            new String[] { "ablk", "aslk", "bar", "foo", "foobar", "tr", "v", "variant" }
        ),
        Arguments.of(
            new String[] { "aslk", "ablk", "", "v", "tr", "", "variant", "foo", "bar", "foobar" },
            0,
            10,
            new String[] { "", "", "ablk", "aslk", "bar", "foo", "foobar", "tr", "v", "variant" }
        )
    );
  }

  @ParameterizedTest
  @MethodSource("getRadixSortParametersProvider")
  void radixSort(String[] values, int fromIndex, int toIndex, String[] expected) {
    StringHelper.radixSort(values, fromIndex, toIndex);
    assertThat(values).usingElementComparator(String::compareTo).isEqualTo(expected);
  }

  @Test
  void radixSortException() {
    assertThatThrownBy(() -> StringHelper.radixSort(new String[] { "", "" }, -23, 1))
        .isInstanceOf(ArrayIndexOutOfBoundsException.class);

    assertThatThrownBy(() -> StringHelper.radixSort(new String[] { "", "" }, 0, 4))
        .isInstanceOf(ArrayIndexOutOfBoundsException.class);

    assertThatThrownBy(() -> StringHelper.radixSort(new String[] { "", "" }, 3, 4))
        .isInstanceOf(ArrayIndexOutOfBoundsException.class);

    assertThatThrownBy(() -> StringHelper.radixSort(new String[] { "", "" }, 3, 2))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
