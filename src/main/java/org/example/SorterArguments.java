package org.example;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Supplier;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@Command(description = "large file sorter")
public class SorterArguments {

  @Option(names = {"--threads"}, description = "count of threads (default: ${DEFAULT-VALUE})")
  private int threadsCount = Runtime.getRuntime().availableProcessors();

  @Option(
      names = {"--chunks"},
      description = "count of chunks (default: 32)"
  )
  private Integer chunksCount;

  @Option(
      names = {"--strings"},
      description = "count of strings in chunk (default: 64)"
  )
  private Integer stringsCount;

  @Option(
      names = {"--buffer-size"},
      description = "buffer size for saving data. Ignored if reflection is enabled "
          + "(default: ${DEFAULT-VALUE})"
  )
  private int bufferSize = 128;

  @Option(names = {"-i", "--input"}, description = "input file", required = true)
  private Path input;

  @Option(names = {"-o", "--output"}, description = "output file", required = true)
  private Path output;

  @Option(names = {"-e", "--encoding"}, description = "encoding for file")
  private String encoding;

  @Option(names = {"--input-encoding"}, description = "encoding for input file")
  private String inputEncoding;

  @Option(names = {"--output-encoding"}, description = "encoding for output file")
  private String outputEncoding;

  @Option(names = { "--no-reflection" }, description = "disable reflection")
  private boolean disableReflection;

  @Option(
      names = { "-m", "--memorySize" },
      description = "calculate 'chunks' and 'strings' using memorySize (memorySize in bytes)"
  )
  private Long memorySize;

  @Option(names = { "-h", "--help" }, description = "display a help message", usageHelp = true)
  private boolean helpRequested;

  public Charset getCharset() {
    return getCharset(encoding, Charset::defaultCharset);
  }

  public Charset getInputCharset() {
    return getCharset(inputEncoding, this::getCharset);
  }

  public Charset getOutputCharset() {
    return getCharset(outputEncoding, this::getCharset);
  }

  private Charset getCharset(String encoding, Supplier<Charset> defaultCharset) {
    return encoding == null || encoding.isBlank()
        ? defaultCharset.get()
        : Charset.forName(encoding);
  }
}
