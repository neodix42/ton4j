package org.ton.ton4j.tl.liteserver.responses;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.blockLinkForward to_key_block:Bool from:tonNode.blockIdExt to:tonNode.blockIdExt
 * dest_proof:bytes config_proof:bytes signatures:liteServer.SignatureSet = liteServer.BlockLink;
 */
@EqualsAndHashCode(callSuper = true)
@Builder
@Data
class BlockLinkForward extends BlockLink {
  public static final int CONSTRUCTOR_ID = 1376767516;

  private final boolean toKeyBlock;
  private final BlockIdExt from;
  private final BlockIdExt to;
  public final byte[] destProof;
  public final byte[] configProof;
  public final byte[] signatures;

  public String getDestProof() {
    if (destProof == null) {
      return "";
    }
    return Utils.bytesToHex(destProof);
  }

  public String getConfigProof() {
    if (configProof == null) {
      return "";
    }
    return Utils.bytesToHex(configProof);
  }

  public String getSignatures() {
    if (signatures == null) {
      return "";
    }
    return Utils.bytesToHex(signatures);
  }

  public static BlockLinkForward deserialize(ByteBuffer buffer) {
    int constructorId = buffer.getInt();
    if (constructorId != CONSTRUCTOR_ID) {
      throw new IllegalArgumentException(
          "Invalid constructor for BlockLinkForward: " + constructorId);
    }

    boolean toKeyBlock = buffer.getInt() == Utils.TL_TRUE;
    BlockIdExt from = BlockIdExt.deserialize(buffer);
    BlockIdExt to = BlockIdExt.deserialize(buffer);
    byte[] destProof = Utils.fromBytes(buffer);
    byte[] configProof = Utils.fromBytes(buffer);
    byte[] signatures = Utils.fromBytes(buffer); // Placeholder

    return BlockLinkForward.builder()
        .toKeyBlock(toKeyBlock)
        .from(from)
        .to(to)
        .destProof(destProof)
        .configProof(configProof)
        .signatures(signatures)
        .build();
  }

  public byte[] serialize() {
    byte[] destProofBytes = Utils.toBytes(destProof);
    byte[] configProofBytes = Utils.toBytes(configProof);
    byte[] signaturesBytes = Utils.toBytes(signatures);

    return ByteBuffer.allocate(
            4
                + 4
                + BlockIdExt.getSize() * 2
                + destProofBytes.length
                + configProofBytes.length
                + signaturesBytes.length)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(CONSTRUCTOR_ID)
        .putInt(toKeyBlock ? Utils.TL_TRUE : Utils.TL_FALSE)
        .put(from.serialize())
        .put(to.serialize())
        .put(destProofBytes)
        .put(configProofBytes)
        .put(signaturesBytes)
        .array();
  }
}
