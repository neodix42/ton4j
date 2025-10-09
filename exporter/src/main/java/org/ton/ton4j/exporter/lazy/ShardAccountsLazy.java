package org.ton.ton4j.exporter.lazy;

import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.*;
import org.ton.ton4j.exporter.reader.CellDbReader;
import org.ton.ton4j.utils.Utils;

/**
 * The key here is the account address
 *
 * <pre>
 * _ (HashmapAugE 256 ShardAccount DepthBalanceInfo) = ShardAccounts;
 * </pre>
 */
@Slf4j
@Builder
@Data
public class ShardAccountsLazy {
  CellSliceLazy rootSlice;
  CellDbReader cellDbReader;

  public Cell toCell() {
    // Not implemented for lazy version
    throw new UnsupportedOperationException("toCell() not supported for lazy ShardAccounts");
  }

  public static ShardAccountsLazy deserialize(CellSliceLazy cs) {
    // Don't deserialize the entire hashmap - just store the root
    return ShardAccountsLazy.builder().rootSlice(cs).cellDbReader(cs.cellDbReader).build();
  }

  public List<ShardAccountLazy> getShardAccountsAsList() {
    // This would load all accounts - not recommended for lazy loading
    throw new UnsupportedOperationException(
        "getShardAccountsAsList() not supported for lazy ShardAccounts - use lookup instead");
  }

  /**
   * Lookup a single account by address using Patricia tree traversal This only loads the cells
   * needed to find the specific account
   */
  public ShardAccountLazy getShardAccountByAddress(Address address) {
    if (rootSlice == null) {
      return null;
    }

    BigInteger key = address.toBigInteger();
    BitString keyBits = new BitString(256);
    keyBits.writeUint(key, 256);

    // Traverse the Patricia tree to find the account
    CellSliceLazy current = rootSlice.clone();
    int keyPos = 0;

    while (keyPos < 256) {
      log.info("key pos {}", keyPos);
      if (keyPos == 17) {
        log.info("breakpoint");
      }
      // Read the label
      BitString label = deserializeLabel(current, 256 - keyPos);

      // Check if label matches our key
      for (int i = 0; i < label.getUsedBits(); i++) {
        if (keyBits.readBit() != label.readBit()) {
          // Key not found
          break;
        }
        keyPos++;
      }

      if (keyPos == 256) {
        // Found the account - current slice contains the value
        return ShardAccountLazy.deserialize(current);
      }

      // Not at leaf yet - need to follow a reference
      // Get the refs count from the cell
      //      Cell edgeCell = current.sliceToCell();
      int refsCount = current.getRefsCountLazy();

      if (refsCount == 0) {
        // No more refs - account not found
        log.info("refsCount 0, account not found");
        return null;
      }

      // Determine which branch to follow (left=0, right=1)
      boolean goRight = keyBits.readBit();
      keyPos++;

      // Calculate which reference to follow
      int refIndex = goRight ? 1 : 0;
      if (refIndex >= refsCount) {
        // Reference doesn't exist
        return null;
      }

      // Load the reference cell
      int hashesPerRef = current.hashes.length / 32 / refsCount;
      byte[] hash = Utils.slice(current.hashes, (refIndex * hashesPerRef * 32), 32);
      Cell refCell = current.getRefByHash(hash);
      current = CellSliceLazy.beginParse(cellDbReader, refCell);
    }

    return null;
  }

  private BitString deserializeLabel(CellSliceLazy edge, int m) {
    if (!edge.loadBit()) {
      // hml_short$0 {m:#} {n:#} len:(Unary ~n) s:(n * Bit) = HmLabel ~n m;
      return deserializeLabelShort(edge);
    }
    if (!edge.loadBit()) {
      // hml_long$10 {m:#} n:(#<= m) s:(n * Bit) = HmLabel ~n m;
      return deserializeLabelLong(edge, m);
    }
    // hml_same$11 {m:#} v:Bit n:(#<= m) = HmLabel ~n m;
    return deserializeLabelSame(edge, m);
  }

  private BitString deserializeLabelShort(CellSliceLazy edge) {
    // Find the length by counting consecutive 1s until we hit a 0
    int length = 0;
    while (length < edge.getRestBits() && edge.preloadBitAt(length + 1)) {
      length++;
    }
    edge.skipBits(length + 1); // Skip the unary length + terminating 0
    return edge.loadBits(length);
  }

  private BitString deserializeLabelLong(CellSliceLazy edge, int m) {
    if (m == 0) {
      return new BitString(0);
    }
    int lenBits = 32 - Integer.numberOfLeadingZeros(m);

    // Check if we have enough bits to read the length field (like C++ fetch_uint_leq)
    if (edge.getRestBits() < lenBits) {

      return new BitString(0);
    }

    BigInteger length = edge.loadUint(lenBits);

    // Validate that the length is within bounds (like C++ fetch_uint_leq validation)
    if (length.intValue() > m) {
      return new BitString(0);
    }

    // Check if we have enough bits to read the actual label data
    if (edge.getRestBits() < length.intValue()) {
      return new BitString(0);
    }

    return edge.loadBits(length.intValue());
  }

  private BitString deserializeLabelSame(CellSliceLazy edge, int m) {
    if (m == 0) {
      return new BitString(0);
    }
    boolean v = edge.loadBit();
    //    int lenBits = 32 - Integer.numberOfLeadingZeros(m);
    int lenBits = m == 0 ? 0 : 32 - Integer.numberOfLeadingZeros(m);

    // Check if we have enough bits to read the length field
    if (edge.getRestBits() < lenBits) {
      return new BitString(0);
    }

    BigInteger length = edge.loadUint(lenBits);
    if (length.intValue() > m) {
      throw new Error("Label length " + length + " exceeds maximum " + m);
    }
    BitString r = new BitString(length.intValue());
    for (int i = 0; i < length.intValue(); i++) {
      r.writeBit(v);
    }
    return r;
  }
}
