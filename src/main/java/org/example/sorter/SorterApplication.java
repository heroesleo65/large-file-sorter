package org.example.sorter;

import org.example.sorter.utils.StringHelper;
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
    var threadsCount = sorterCommand.getThreadsCount();

    var chunksCount = sorterCommand.getChunksCount();
    var chunkSize = sorterCommand.getStringsCount();
    var bufferSize = sorterCommand.getBufferSize();

    try (var fileSorter = new FileSorter(input, charset, threadsCount)) {
      fileSorter.sort(chunksCount, chunkSize, bufferSize, output);
    } catch (InterruptedException ex) {
      // ignore
    }
  }
}
