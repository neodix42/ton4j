package org.ton.ton4j.tl.liteserver.responses;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.blockLinkBack to_key_block:Bool from:tonNode.blockIdExt to:tonNode.blockIdExt
 * dest_proof:bytes proof:bytes state_proof:bytes = liteServer.BlockLink;
 */
@EqualsAndHashCode(callSuper = true)
@Builder
@Data
class BlockLinkBack extends BlockLink {
  public static final int CONSTRUCTOR_ID = -276947985;

  private final boolean toKeyBlock;
  private final BlockIdExt from;
  private final BlockIdExt to;
  public final byte[] destProof;
  public final byte[] proof;
  public final byte[] stateProof;

  public String getDestProof() {
    if (destProof == null) {
      return "";
    }
    return Utils.bytesToHex(destProof);
  }

  public String getProof() {
    if (proof == null) {
      return "";
    }
    return Utils.bytesToHex(proof);
  }

  public String getStateProof() {
    if (stateProof == null) {
      return "";
    }
    return Utils.bytesToHex(stateProof);
  }

  public static BlockLinkBack deserialize(ByteBuffer buffer) {
    int constructorId = buffer.getInt();
    if (constructorId != CONSTRUCTOR_ID) {
      throw new IllegalArgumentException("Invalid constructor for BlockLinkBack: " + constructorId);
    }

    boolean toKeyBlock = buffer.getInt() == Utils.TL_TRUE;
    BlockIdExt from = BlockIdExt.deserialize(buffer);
    BlockIdExt to = BlockIdExt.deserialize(buffer);
    byte[] destProof = Utils.fromBytes(buffer);
    byte[] proof = Utils.fromBytes(buffer);
    byte[] stateProof = Utils.fromBytes(buffer);

    return BlockLinkBack.builder()
        .toKeyBlock(toKeyBlock)
        .from(from)
        .to(to)
        .destProof(destProof)
        .proof(proof)
        .stateProof(stateProof)
        .build();
  }

  public byte[] serialize() {
    byte[] destProofBytes = Utils.toBytes(destProof);
    byte[] proofBytes = Utils.toBytes(proof);
    byte[] stateProofBytes = Utils.toBytes(stateProof);

    return ByteBuffer.allocate(
            4
                + 4
                + BlockIdExt.getSize() * 2
                + destProofBytes.length
                + proofBytes.length
                + stateProofBytes.length)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(CONSTRUCTOR_ID)
        .putInt(toKeyBlock ? Utils.TL_TRUE : Utils.TL_FALSE)
        .put(from.serialize())
        .put(to.serialize())
        .put(destProofBytes)
        .put(proofBytes)
        .put(stateProofBytes)
        .array();
  }
}
