package org.example;

import static org.example.sorter.parameters.DefaultParameters.MIN_AVAILABLE_CHUNKS;
import static org.example.sorter.parameters.DefaultParameters.MIN_CHUNK_SIZE;
import static org.example.sorter.parameters.DefaultParameters.MIN_MEMORY_SIZE;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.example.context.DefaultApplicationContext;
import org.example.sorter.FileSorter;
import org.example.sorter.parameters.ChunkParameters;
import org.example.sorter.parameters.formula.QuadraticParameterFormula;
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
    Comparator<String> comparator = sorterCommand.isIgnoreCase()
        ? String.CASE_INSENSITIVE_ORDER
        : Comparator.naturalOrder();

    var context = new DefaultApplicationContext(
        /* prefixTemporaryDirectory = */ null, sorterCommand.isEnableReflection()
    );
    try (var fileSorter = new FileSorter(input, inputCharset, threadsCount, context)) {
      var formula = new QuadraticParameterFormula();
      var parameters = new ChunkParameters(
          availableChunks, chunkSize, bufferSize, memorySize, formula
      );
      fileSorter.sort(parameters, comparator, output, outputCharset);
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
        () -> isEmptyOr(arguments.getChunksCount(), value -> value >= MIN_AVAILABLE_CHUNKS)
            ? null
            : "--chunks",
        () -> isEmptyOr(arguments.getStringsCount(), value -> value >= MIN_CHUNK_SIZE)
            ? null
            : "--strings",
        () -> isEmptyOr(arguments.getMemorySize(), value -> value >= MIN_MEMORY_SIZE)
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

  private static <T> boolean isEmptyOr(T value, Predicate<T> predicate) {
    return value == null || predicate.test(value);
  }
}
