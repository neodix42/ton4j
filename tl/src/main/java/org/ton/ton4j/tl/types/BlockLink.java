package org.ton.ton4j.tl.types;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.utils.Utils;

public abstract class BlockLink {
  public static BlockLink deserialize(ByteBuffer buffer) {
    int constructorId = buffer.getInt();
    buffer.position(buffer.position() - 4);

    if (constructorId == BlockLinkBack.CONSTRUCTOR_ID) {
      return BlockLinkBack.deserialize(buffer);
    } else if (constructorId == BlockLinkForward.CONSTRUCTOR_ID) {
      return BlockLinkForward.deserialize(buffer);
    } else {
      throw new IllegalArgumentException("Unknown BlockLink constructor: " + constructorId);
    }
  }

  public abstract byte[] serialize();
}

@Getter
@Builder
class BlockLinkBack extends BlockLink {
  public static final int CONSTRUCTOR_ID = -276947985; // Placeholder CRC32

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

@Getter
@Builder
class BlockLinkForward extends BlockLink {
  public static final int CONSTRUCTOR_ID = 1376767516; // Placeholder CRC32

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
