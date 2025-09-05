package org.ton.ton4j.exporter.types;

public class StateFileInfo {
  public StateFileType type;
  public int workchain;
  public String shard;
  public long seqno;
  public String effectiveShard; // Only for split account states
  public String rootHash;
  public String fileHash;
  public String filePath;

  @Override
  public String toString() {
    return String.format(
        "StateFileInfo{type=%s, workchain=%d, shard=%s, seqno=%d, effectiveShard=%s, rootHash=%s, fileHash=%s}",
        type,
        workchain,
        shard,
        seqno,
        effectiveShard,
        rootHash != null ? rootHash.substring(0, Math.min(8, rootHash.length())) + "..." : null,
        fileHash != null ? fileHash.substring(0, Math.min(8, fileHash.length())) + "..." : null);
  }
}
