package org.example.sorter;

import picocli.CommandLine;

public class SorterApplication {
  public static void main(String[] args) {
    var sorterCommand = new SorterArguments();
    var commandLine = new CommandLine(sorterCommand);
    commandLine.parseArgs(args);

    if (commandLine.isUsageHelpRequested()) {
      commandLine.usage(System.out);
      return;
    }

    if (sorterCommand.isDisableReflection()) {
      StringHelper.disableReflection();
    } else {
      StringHelper.checkSupportReflection();
    }

    var input = sorterCommand.getInput();
    var output = sorterCommand.getOutput();
    var charset = sorterCommand.getCharset();
    var threadsCount = getThreadsCount(sorterCommand.getThreadsCount());

    var chunksCount = sorterCommand.getChunksCount();
    var chunkSize = sorterCommand.getStringsCount();

    try (var fileSorter = new FileSorter(input, charset, threadsCount)) {
      fileSorter.sort(chunksCount, chunkSize, output);
    } catch (InterruptedException ex) {
      // ignore
    }
  }

  private static int getThreadsCount(Integer threadsCount) {
    return threadsCount != null ? threadsCount : Runtime.getRuntime().availableProcessors();
  }
}
