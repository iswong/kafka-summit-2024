package ks2004.demo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import lombok.Builder;
import org.apache.flink.api.common.serialization.BulkWriter;
import org.apache.flink.core.fs.FSDataOutputStream;
import org.apache.flink.util.Preconditions;

public class CsvBulkWriter<T> implements BulkWriter<T> {

  private final FSDataOutputStream stream;
  private final Function<T, List<String[]>> converter;

  @Builder
  CsvBulkWriter(final FSDataOutputStream stream, final Function<T, List<String[]>> converter) {
    this.stream = Preconditions.checkNotNull(stream);
    this.converter = converter;

  }

  public void addElement(T element) throws IOException {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    final List<String[]> lines = converter.apply(element);
    for (String[] line : lines) {
      bytes.write((String.join(",", line) + System.lineSeparator())
          .getBytes(StandardCharsets.UTF_8));
    }
    this.stream.write(bytes.toByteArray());
  }

  public void flush() throws IOException {
    this.stream.flush();
  }

  public void finish() throws IOException {
    this.stream.sync();
  }
}
