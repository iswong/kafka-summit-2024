package ks2004.demo;

import org.apache.flink.api.common.serialization.BulkWriter;
import org.apache.flink.core.fs.FSDataOutputStream;
import org.apache.flink.formats.common.Converter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonGenerator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.flink.util.Preconditions;
import org.apache.flink.util.jackson.JacksonMapperFactory;

import javax.annotation.Nullable;
import java.io.IOException;

public class CsvBulkWriter<T, R, C> implements BulkWriter<T> {
    private final FSDataOutputStream stream;
    private final Converter<T, R, C> converter;
    @Nullable
    private final C converterContext;
    private final ObjectWriter csvWriter;

    CsvBulkWriter(CsvMapper mapper, CsvSchema schema, Converter<T, R, C> converter, @Nullable C converterContext, FSDataOutputStream stream) {
        Preconditions.checkNotNull(mapper);
        Preconditions.checkNotNull(schema);
        this.converter = (Converter)Preconditions.checkNotNull(converter);
        this.stream = (FSDataOutputStream)Preconditions.checkNotNull(stream);
        this.converterContext = converterContext;
        this.csvWriter = mapper.writer(schema);
        mapper.getFactory().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    }

    public static <T, R, C> CsvBulkWriter<T, R, C> forSchema(CsvMapper mapper, CsvSchema schema, Converter<T, R, C> converter, @Nullable C converterContext, FSDataOutputStream stream) {
        return new CsvBulkWriter(mapper, schema, converter, converterContext, stream);
    }

    public static <T> CsvBulkWriter<T, T, Void> forPojo(Class<T> pojoClass, FSDataOutputStream stream) {
        Converter<T, T, Void> converter = (value, context) -> {
            return value;
        };
        CsvMapper csvMapper = JacksonMapperFactory.createCsvMapper();
        CsvSchema schema = csvMapper.schemaFor(pojoClass).withoutQuoteChar();
        return new CsvBulkWriter(csvMapper, schema, converter, (Object)null, stream);
    }

    public void addElement(T element) throws IOException {
        R r = this.converter.convert(element, this.converterContext);
        this.csvWriter.writeValue(this.stream, r);
    }

    public void flush() throws IOException {
        this.stream.flush();
    }

    public void finish() throws IOException {
        this.stream.sync();
    }
}
