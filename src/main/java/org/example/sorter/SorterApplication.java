package org.example.sorter;

import org.example.context.DefaultApplicationContext;
import picocli.CommandLine;

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

    if (commandLine.isUsageHelpRequested()) {
      commandLine.usage(System.out);
      return;
    }

    var input = sorterCommand.getInput();
    var output = sorterCommand.getOutput();
    var charset = sorterCommand.getCharset();
    var threadsCount = sorterCommand.getThreadsCount();

    var availableChunks = sorterCommand.getChunksCount();
    var chunkSize = sorterCommand.getStringsCount();
    var bufferSize = sorterCommand.getBufferSize();

    var context = new DefaultApplicationContext(sorterCommand.isDisableReflection());
    try (var fileSorter = new FileSorter(input, charset, threadsCount, context)) {
      fileSorter.sort(new ChunkParameters(availableChunks, chunkSize, bufferSize), output);
    } catch (InterruptedException ex) {
      // ignore
    }
  }
}
