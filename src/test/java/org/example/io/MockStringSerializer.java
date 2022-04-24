package org.example.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MockStringSerializer implements StringSerializer {

  private final List<Args> args = new ArrayList<>();

  @Override
  public void write(OutputStream stream, String[] data, int from, int to) {
    args.add(new Args(stream, data, from, to));
  }

  public void verify(int times, OutputStream stream, String[] data, int from, int to) {
    var arg = new Args(stream, data, from, to);
    if (times == 0) {
      assertThat(args).doesNotContain(arg);
    } else {
      assertThat(args).contains(arg);
      assertThat(args.stream().filter(arg::equals).count()).isEqualTo(times);
    }
  }

  private static class Args {
    private final OutputStream stream;
    private final String[] data;
    private final int from;
    private final int to;

    public Args(OutputStream stream, String[] data, int from, int to) {
      this.stream = stream;
      this.data = data.clone();
      this.from = from;
      this.to = to;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Args args = (Args) o;
      return from == args.from && to == args.to && Objects.equals(stream, args.stream)
          && Arrays.equals(data, args.data);
    }

    @Override
    public String toString() {
      return "{"
          + "stream=" + stream
          + ", data=" + Arrays.toString(data)
          + ", from=" + from
          + ", to=" + to
          + '}';
    }
  }
}
