package ks2004.demo;

import java.io.IOException;
import org.apache.flink.streaming.api.functions.sink.filesystem.PartFileInfo;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.CheckpointRollingPolicy;

class OnEventCheckpointRollingPolicy extends CheckpointRollingPolicy {

  @Override
  public boolean shouldRollOnEvent(final PartFileInfo partFileState, final Object element)
      throws IOException {
    return true;
  }

  @Override
  public boolean shouldRollOnProcessingTime(final PartFileInfo partFileState,
      final long currentTime)
      throws IOException {
    return false;
  }
}
