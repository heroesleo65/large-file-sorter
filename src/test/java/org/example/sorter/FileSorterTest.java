package org.example.sorter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.log4j.Log4j2;
import org.example.context.ApplicationContext;
import org.example.context.DefaultStringContext;
import org.example.context.FileSystemContext;
import org.example.io.MockOutputStream;
import org.example.io.MockRandomAccessInputStream;
import org.example.io.StreamFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@Log4j2
class FileSorterTest {

  private static final ClassLoader loader = FileSorterTest.class.getClassLoader();

  @ParameterizedTest
  @CsvSource({
      "small-input.txt, 1, 4, 4, 128, true",
      "small-input.txt, 2, 4, 4, 128, true",
      "small-input.txt, 1, 32, 32, 128, true",
      "small-input.txt, 2, 32, 32, 128, true",

      "small-input.txt, 1, 4, 4, 128, false",
      "small-input.txt, 2, 4, 4, 128, false",
      "small-input.txt, 1, 32, 32, 128, false",
      "small-input.txt, 2, 32, 32, 128, false",

      "middle-input.txt, 1, 4, 4, 128, true",
      "middle-input.txt, 2, 4, 4, 128, true",
      "middle-input.txt, 1, 32, 32, 128, true",
      "middle-input.txt, 2, 32, 32, 128, true",

      "middle-input.txt, 1, 4, 4, 128, false",
      "middle-input.txt, 2, 4, 4, 128, false",
      "middle-input.txt, 1, 32, 32, 128, false",
      "middle-input.txt, 2, 32, 32, 128, false",
  })
  void integrationSortInMemory(
      String resourceName,
      int threadsCount,
      int availableChunks,
      int chunkSize,
      int bufferSize,
      boolean reflectionFlag
  ) throws Exception {
    String text = loadResource(resourceName);

    var input = Path.of("input");
    var output = Path.of("output");
    var counter = new AtomicInteger();

    Map<File, ByteArrayOutputStream> outputStreams = new ConcurrentHashMap<>();

    var existsFileAnswer = new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) {
        var file = invocation.getArgument(0, File.class);
        return outputStreams.containsKey(file);
      }
    };

    var outputStreamFactory = mock(StreamFactory.class);
    var fileSystemContext = mock(FileSystemContext.class);
    var stringContext = new DefaultStringContext(reflectionFlag);
    var context = spy(ApplicationContext.class);

    when(context.getStreamFactory()).thenReturn(outputStreamFactory);
    when(context.getFileSystemContext()).thenReturn(fileSystemContext);
    when(context.getStringContext()).thenReturn(stringContext);

    when(fileSystemContext.isFile(any())).thenAnswer(existsFileAnswer);
    when(fileSystemContext.canRead(any())).thenAnswer(existsFileAnswer);
    when(fileSystemContext.exists(any())).thenAnswer(existsFileAnswer);
    when(fileSystemContext.delete(any())).thenReturn(true);
    when(fileSystemContext.nextTemporaryFile()).thenAnswer(invocation -> counter.getAndIncrement());
    when(fileSystemContext.getTemporaryFile(anyInt())).thenAnswer(invocation -> {
      int id = invocation.getArgument(0, Integer.class);
      return new File(String.valueOf(id));
    });

    when(outputStreamFactory.getBufferedReader(eq(input), any()))
      .thenAnswer(invocation -> new BufferedReader(new StringReader(text)));
    when(outputStreamFactory.getOutputStream(any())).thenAnswer(invocation -> {
      var file = invocation.getArgument(0, File.class);
      var outputStream = outputStreams.computeIfAbsent(file, f -> new ByteArrayOutputStream());
      return new MockOutputStream(outputStream);
    });
    when(outputStreamFactory.getRandomAccessInputStream(any())).thenAnswer(invocation -> {
      var file = invocation.getArgument(0, File.class);
      var outputStream = outputStreams.get(file);
      if (outputStream == null) {
        throw new FileNotFoundException();
      }

      return new MockRandomAccessInputStream(outputStream.toByteArray());
    });

    try (var sorter = new FileSorter(input, UTF_8, threadsCount, context)) {
      sorter.sort(new ChunkParameters(availableChunks, chunkSize, bufferSize), output, UTF_8);
    }

    if (reflectionFlag != stringContext.hasSupportReflection()) {
      log.warn("Reflection feature was changed to {}", !reflectionFlag);
    }

    var resultOutputStream = outputStreams.get(output.toFile());

    assertThat(resultOutputStream).isNotNull();

    var actual = resultOutputStream.toString(UTF_8).lines().collect(toList());
    var expected = text.lines().sorted().collect(toList());

    assertThat(actual).containsExactlyElementsOf(expected);
  }

  private String loadResource(String name) throws IOException {
    try (var inputStream = loader.getResourceAsStream(name)) {
      assert inputStream != null;
      return new String(inputStream.readAllBytes(), UTF_8);
    }
  }
}