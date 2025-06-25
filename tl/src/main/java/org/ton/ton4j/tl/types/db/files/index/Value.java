package org.ton.ton4j.tl.types.db.files.index;

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
 * db.files.index.value packages:(vector int) key_packages:(vector int) temp_packages:(vector int) = db.files.index.Value;
 * </pre>
 */
@Builder
@Data
public class Value implements Serializable {

  List<Integer> packages;
  List<Integer> keyPackages;
  List<Integer> tempPackages;

  public static Value deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    
    // Read packages vector
    int packagesCount = buffer.getInt();
    List<Integer> packages = new ArrayList<>(packagesCount);
    for (int i = 0; i < packagesCount; i++) {
      packages.add(buffer.getInt());
    }
    
    // Read key_packages vector
    int keyPackagesCount = buffer.getInt();
    List<Integer> keyPackages = new ArrayList<>(keyPackagesCount);
    for (int i = 0; i < keyPackagesCount; i++) {
      keyPackages.add(buffer.getInt());
    }
    
    // Read temp_packages vector
    int tempPackagesCount = buffer.getInt();
    List<Integer> tempPackages = new ArrayList<>(tempPackagesCount);
    for (int i = 0; i < tempPackagesCount; i++) {
      tempPackages.add(buffer.getInt());
    }
    
    return Value.builder()
        .packages(packages)
        .keyPackages(keyPackages)
        .tempPackages(tempPackages)
        .build();
  }

  public byte[] serialize() {
    // Calculate buffer size
    int size = 4 + packages.size() * 4 + 4 + keyPackages.size() * 4 + 4 + tempPackages.size() * 4;
    
    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    
    // Write packages vector
    buffer.putInt(packages.size());
    for (Integer pkg : packages) {
      buffer.putInt(pkg);
    }
    
    // Write key_packages vector
    buffer.putInt(keyPackages.size());
    for (Integer pkg : keyPackages) {
      buffer.putInt(pkg);
    }
    
    // Write temp_packages vector
    buffer.putInt(tempPackages.size());
    for (Integer pkg : tempPackages) {
      buffer.putInt(pkg);
    }
    
    return buffer.array();
  }
}
