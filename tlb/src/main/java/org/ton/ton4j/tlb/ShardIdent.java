package org.ton.ton4j.tlb;

import static java.util.Objects.isNull;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * shard_ident$00
 *  shard_pfx_bits:(#<= 60)
 *  workchain_id:int32
 *  shard_prefix:uint64
 *  = ShardIdent;
 * </pre>
 */
@Builder
@Data
public class ShardIdent {
  long magic;
  int prefixBits;
  int workchain;
  BigInteger shardPrefix;

  private String getMagic() {
    return Long.toBinaryString(magic);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0, 2)
        .storeUint(prefixBits, 6)
        .storeInt(workchain, 32)
        .storeUint(shardPrefix, 64)
        .endCell(); // 104 bits
  }

  public static ShardIdent deserialize(CellSlice cs) {
    long magic = cs.loadUint(2).longValue();
    assert (magic == 0b00)
        : "ShardIdent: magic not equal to 0b00, found 0b" + Long.toBinaryString(magic);
    return ShardIdent.builder()
        .magic(0L)
        .prefixBits(cs.loadUint(6).intValue())
        .workchain(cs.loadInt(32).intValue())
        .shardPrefix(cs.loadUint(64))
        .build();
  }

  public BigInteger convertShardIdentToShard() {
    if (isNull(shardPrefix)) {
      throw new Error("Shard prefix is null, should be in range 0..60");
    }
    if (shardPrefix.compareTo(BigInteger.valueOf(60)) > 0) {
      return shardPrefix;
    }
    return BigInteger.valueOf(2)
        .multiply(shardPrefix)
        .add(BigInteger.ONE)
        .shiftLeft(63 - prefixBits);
  }

  public BigInteger getParent() {
    BigInteger shard = convertShardIdentToShard();
    BigInteger x = lowerBit64(shard);

    BigInteger a = shard.subtract(x);
    BigInteger b = x.shiftLeft(1);
    if (b.longValue() < 0) {
      return a.or(b);
    } else {
      return BigInteger.valueOf(a.longValue() | b.longValue());
    }
  }

  public BigInteger getChildRight() {
    BigInteger shard = convertShardIdentToShard();
    BigInteger x = lowerBit64(shard).shiftRight(1);
    return shard.add(x);
  }

  public BigInteger getChildLeft() {
    BigInteger shard = convertShardIdentToShard();
    BigInteger x = lowerBit64(shard).shiftRight(1);
    return shard.subtract(x);
  }

  private BigInteger lowerBit64(BigInteger x) {
    return x.and(bitsNegate64(x));
  }

  public BigInteger bitsNegate64(BigInteger x) {
    // Create a mask for 64 bits (all bits set to 1)
    BigInteger mask = new BigInteger("FFFFFFFFFFFFFFFF", 16);
    // Apply bitwise NOT using the mask to emulate 64-bit unsigned behavior
    return x.not().and(mask).add(BigInteger.ONE);
  }

  public static BigInteger ROOT() {

    return BigInteger.ONE.shiftLeft(63);
  }

  public static ShardIdent convertShardToShardIdent(String shard, int workchain) {

    return ShardIdent.builder().workchain(workchain).shardPrefix(new BigInteger(shard, 16)).build();
  }
}
