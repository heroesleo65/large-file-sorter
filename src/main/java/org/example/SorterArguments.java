package org.example;

import static org.example.sorter.parameters.DefaultParameters.DEFAULT_AVAILABLE_CHUNKS;
import static org.example.sorter.parameters.DefaultParameters.DEFAULT_BUFFER_SIZE;
import static org.example.sorter.parameters.DefaultParameters.DEFAULT_CHUNK_SIZE;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Supplier;
import lombok.Getter;
import org.example.utils.DataSizeHelper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@Command(description = "large file sorter")
public class SorterArguments {

  @Option(names = {"--threads"}, description = "count of threads (default: ${DEFAULT-VALUE})")
  private int threadsCount = 2 * Runtime.getRuntime().availableProcessors();

  @Option(
      names = {"--chunks"},
      description = "count of chunks (default: " + DEFAULT_AVAILABLE_CHUNKS + ")"
  )
  private Integer chunksCount;

  @Option(
      names = {"--strings"},
      description = "count of strings in chunk (default: " + DEFAULT_CHUNK_SIZE + ")"
  )
  private Integer stringsCount;

  @Option(
      names = {"--buffer-size"},
      description = "buffer size for saving data. (default: ${DEFAULT-VALUE})"
  )
  private String bufferSize = String.valueOf(DEFAULT_BUFFER_SIZE);

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

  @Option(names = {"--reflection"}, description = "enable reflection (fast mode)")
  private boolean enableReflection;

  @Option(names = {"-I", "--ignore-case"}, description = "ignore case in compare of two strings")
  private boolean ignoreCase;

  @Option(names = {"--verbose"}, description = "show details information about process")
  private boolean verbose;

  @Option(
      names = {"-m", "--memory-size"},
      description = "calculate 'chunks' and 'strings' using memorySize"
  )
  private String memorySize;

  @Option(names = {"-h", "--help"}, description = "display a help message", usageHelp = true)
  private boolean helpRequested;

  public Charset getCharset() {
    return getCharset(encoding, Charset::defaultCharset);
  }

  private Charset getCharset(String encoding, Supplier<Charset> defaultCharset) {
    return encoding == null || encoding.isBlank()
        ? defaultCharset.get()
        : Charset.forName(encoding);
  }

  public Charset getInputCharset() {
    return getCharset(inputEncoding, this::getCharset);
  }

  public Charset getOutputCharset() {
    return getCharset(outputEncoding, this::getCharset);
  }

  public int getBufferSize() {
    return (int) DataSizeHelper.parse(bufferSize).toBytes();
  }

  public Long getMemorySize() {
    if (memorySize == null || memorySize.isBlank()) {
      return null;
    }
    return DataSizeHelper.parse(memorySize).toBytes();
  }
}
