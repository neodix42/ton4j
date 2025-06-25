package org.ton.ton4j.tl.types.db.state;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.state.destroyedSessions sessions:(vector int256) = db.state.DestroyedSessions;
 * </pre>
 */
@Builder
@Data
public class DestroyedSessions implements Serializable {

  List<byte[]> sessions;  // vector of int256

  // Convenience getter
  public List<String> getSessionsHex() {
    List<String> result = new ArrayList<>(sessions.size());
    for (byte[] session : sessions) {
      result.add(Utils.bytesToHex(session));
    }
    return result;
  }

  public static DestroyedSessions deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    
    // Read sessions vector
    int sessionsCount = buffer.getInt();
    List<byte[]> sessions = new ArrayList<>(sessionsCount);
    for (int i = 0; i < sessionsCount; i++) {
      sessions.add(Utils.read(buffer, 32));  // int256 is 32 bytes
    }
    
    return DestroyedSessions.builder()
        .sessions(sessions)
        .build();
  }

  public byte[] serialize() {
    // Calculate buffer size
    int size = 4 + (sessions.size() * 32);  // 4 bytes for vector size + 32 bytes per int256
    
    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    
    // Write sessions vector
    buffer.putInt(sessions.size());
    for (byte[] session : sessions) {
      buffer.put(session);
    }
    
    return buffer.array();
  }
}
