package org.example;

import static org.example.sorter.parameters.DefaultParameters.MIN_AVAILABLE_CHUNKS;
import static org.example.sorter.parameters.DefaultParameters.MIN_CHUNK_SIZE;
import static org.example.sorter.parameters.DefaultParameters.RESERVED_FOR_SYSTEM_MEMORY_SIZE;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.example.context.DefaultApplicationContext;
import org.example.sorter.FileSorter;
import org.example.sorter.parameters.ChunkParameters;
import org.example.sorter.parameters.formula.QuadraticParametersFormula;
import org.example.utils.TerminalHelper;
import picocli.CommandLine;

@Log4j2
public class SorterApplication {
  public static void main(String[] args) {
    var sorterCommand = new SorterArguments();
    var commandLine = new CommandLine(sorterCommand);

    try {
      commandLine.parseArgs(args);
    } catch (Exception ex) {
      commandLine.usage(System.out);
      return;
    }

    if (commandLine.isUsageHelpRequested() || !check(sorterCommand)) {
      commandLine.usage(System.out);
      return;
    }

    var input = sorterCommand.getInput();
    var output = sorterCommand.getOutput();
    var inputCharset = sorterCommand.getInputCharset();
    var outputCharset = sorterCommand.getOutputCharset();
    var threadsCount = sorterCommand.getThreadsCount();

    var availableChunks = sorterCommand.getChunksCount();
    var chunkSize = sorterCommand.getStringsCount();
    var bufferSize = sorterCommand.getBufferSize();
    var memorySize = sorterCommand.getMemorySize();

    var context = new DefaultApplicationContext(
        /* prefixTemporaryDirectory = */ null, !sorterCommand.isDisableReflection()
    );
    try (var fileSorter = new FileSorter(input, inputCharset, threadsCount, context)) {
      var formula = new QuadraticParametersFormula();
      var parameters = new ChunkParameters(
          availableChunks, chunkSize, bufferSize, memorySize, formula
      );
      fileSorter.sort(parameters, output, outputCharset);
    } catch (InterruptedException ex) {
      TerminalHelper.forceCloseTerminal();
    } catch (Exception ex) {
      log.error("Happened bad situation", ex);
    }
  }

  private static boolean check(SorterArguments arguments) {
    List<String> invalidValues = check(
        () -> arguments.getThreadsCount() > 0 ? null : "--threads",
        () -> arguments.getBufferSize() > 0 ? null : "--buffer-size",
        () ->
            arguments.getChunksCount() == null || arguments.getChunksCount() >= MIN_AVAILABLE_CHUNKS
                ? null
                : "--chunks",
        () -> arguments.getStringsCount() == null || arguments.getStringsCount() >= MIN_CHUNK_SIZE
            ? null
            : "--strings",
        () -> arguments.getMemorySize() == null
            || arguments.getMemorySize() >= 2 * RESERVED_FOR_SYSTEM_MEMORY_SIZE
            ? null
            : "--memorySize"
    );
    if (!invalidValues.isEmpty()) {
      System.out.format("Invalid parameter(s): '%s'\n", invalidValues);
    }
    return invalidValues.isEmpty();
  }

  private static List<String> check(Supplier<String>... predicates) {
    List<String> result = new ArrayList<>();
    for (var predicate : predicates) {
      var value = predicate.get();
      if (value != null && !value.isBlank()) {
        result.add(value);
      }
    }
    return result;
  }
}
