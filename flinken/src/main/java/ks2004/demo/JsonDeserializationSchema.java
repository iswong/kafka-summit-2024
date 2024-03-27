package ks2004.demo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;

import java.io.IOException;

public class JsonDeserializationSchema<T> implements DeserializationSchema<T> {

    private ObjectMapper objectMapper;

    public Class<T> typeReference;

    public JsonDeserializationSchema(Class<T> typeReference) {
        this.typeReference = typeReference;
    }

    @Override
    public T deserialize(byte[] bytes) throws IOException {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return objectMapper.readValue(bytes, this.typeReference);
    }

    @Override
    public boolean isEndOfStream(T t) {
        return false;
    }

    @Override
    public TypeInformation<T> getProducedType() {
        return TypeExtractor.createTypeInfo(typeReference);
    }
}
