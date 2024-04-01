package ks2004.demo;

import java.time.Instant;
import ks2004.demo.model.ExposureSnapshot;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.streaming.api.functions.sink.filesystem.BucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.SimpleVersionedStringSerializer;

class CustomBucketAssigner implements
    BucketAssigner<ExposureSnapshot, String> {

  @Override
  public String getBucketId(final ExposureSnapshot element, final Context context) {
    return "/" + element.getAccountCode() + "/" + Instant.ofEpochMilli(context.timestamp());
  }

  @Override
  public SimpleVersionedSerializer<String> getSerializer() {
    return SimpleVersionedStringSerializer.INSTANCE;
  }
}
