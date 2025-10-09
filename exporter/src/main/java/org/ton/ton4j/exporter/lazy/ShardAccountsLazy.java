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
    //    byte[] shardAccountsKeyHash = Utils.slice(cs.getHashes(), 0, 32);
    //    Cell shardAccountsCell = cs.getRefByHash(shardAccountsKeyHash);
    //    CellSliceLazy cs1 = CellSliceLazy.beginParse(cs.cellDbReader, shardAccountsCell);

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

    // HashmapAugE structure: first bit indicates if empty (0) or non-empty (1)
    // ahme_empty$0 {n:#} {X:Type} {Y:Type} extra:Y = HashmapAugE n X Y;
    // ahme_root$1 {n:#} {X:Type} {Y:Type} root:^(HashmapAug n X Y) extra:Y = HashmapAugE n X Y;
    //    CellSliceLazy wrapper = rootSlice.clone();
    boolean nonEmpty = rootSlice.loadBit();

    if (!nonEmpty) {
      // Empty hashmap
      return null;
    }

    // Load the actual root cell from the reference
    //    int wrapperRefsCount = rootSlice.getRefsCountLazy();
    if (rootSlice.getRefsCountLazy() < 1) {
      log.error("HashmapAugE marked as non-empty but has no root reference");
      return null;
    }

    // find by hash and load non-empty dictAugE
    byte[] rootHash = Utils.slice(rootSlice.hashes, 0, 32);
    Cell rootCell = rootSlice.getRefByHash(rootHash);

    int keyLength = 256;
    BigInteger key = address.toBigInteger();
    BitString keyBits = new BitString(keyLength);
    keyBits.writeUint(key, keyLength);
    // Reset read position to start
    keyBits.readCursor = 0;

    // Traverse the Patricia tree to find the account, starting from the actual root
    CellSliceLazy current = CellSliceLazy.beginParse(cellDbReader, rootCell);
    int keyPos = 0;

    while (true) {
      log.info("keypos: {}", keyPos);
      if (keyPos == 16) {
        log.debug("debug");
      }
      // https://github.com/ton-blockchain/ton/blob/5a1d271b9b0dd2bbffea555561cdc385ed2672a6/crypto/vm/dict.cpp#L458
      // Read the label
      BitString label = deserializeLabel(current, keyLength);

      // Check if label is a prefix of the remaining key (like C++ is_prefix_of)
      // Reset label read cursor to compare from start
      label.readCursor = 0;
      boolean labelMatches = true;
      for (int i = 0; i < label.getUsedBits(); i++) {
        if (i == 100) {
          log.info("debug 2");
        }
        if (keyBits.readBit() != label.readBit()) {
          // Key not found - label doesn't match as prefix
          labelMatches = false;
          break;
        }
        keyPos++;
      }

      keyLength -= label.getUsedBits(); // or // left bits

      //      if (!labelMatches) {
      //        log.info("Label mismatch at keyPos {}", keyPos);
      //        return null;
      //      }

      int remainingAfterLabel = keyLength - keyPos;
      log.info(
          "Label matched, label length: {}, keyPos now: {}, remaining: {}",
          label.getUsedBits(),
          keyPos,
          remainingAfterLabel);

      // After consuming the label, check if we've reached the end (leaf node)
      // In Patricia tree/HashmapAug, if no bits remain after the label, we're at a leaf
      if (remainingAfterLabel == 0) {
        // Found the account - current slice contains: extra:Y value:X
        // We need to skip the extra (DepthBalanceInfo) to get to the value (ShardAccount)
        // DepthBalanceInfo is: depth_balance$_ split_depth:(#<= 30) balance:CurrencyCollection
        // Skip split_depth (5 bits for #<= 30)
        current.loadUint(5);
        // Skip balance (CurrencyCollection = Grams + ExtraCurrencyCollection)
        skipCurrencyCollection(current);
        // Now current points to the ShardAccount value
        return ShardAccountLazy.deserialize(current);
      }

      // Not at leaf yet - need to follow a reference
      // Get the refs count from the cell
      int refsCount = current.getRefsCountLazy();

      if (refsCount == 0) {
        log.error("Fork nodes must have exactly 2 references, found {}", refsCount);
        return null;
      }

      // Determine which branch to follow (left=0, right=1)
      boolean goRight = keyBits.readBit();
      keyPos++;

      // Calculate which reference to follow
      int refIndex = goRight ? 1 : 0;

      // Load the reference cell
      int hashesPerRef = current.hashes.length / 32 / refsCount;
      byte[] hash = Utils.slice(current.hashes, (refIndex * hashesPerRef * 32), 32);
      Cell refCell = current.getRefByHash(hash);
      current = CellSliceLazy.beginParse(cellDbReader, refCell);
    }
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

  /**
   * Skip CurrencyCollection = Grams + ExtraCurrencyCollection Grams = VarUInteger 16
   * ExtraCurrencyCollection = HashmapE 32 (VarUInteger 32)
   */
  private void skipCurrencyCollection(CellSliceLazy cs) {
    // Skip Grams (VarUInteger 16)
    skipVarUInteger(cs, 16);
    // Skip ExtraCurrencyCollection (HashmapE 32 ...)
    // HashmapE starts with a bit: 0 = empty, 1 = has root
    boolean hasExtra = cs.loadBit();
    if (hasExtra) {
      // Has extra currencies - skip the reference
      cs.loadRef();
    }
  }

  /** Skip VarUInteger n var_uint$_ {n:#} len:(#< n) value:(uint (len * 8)) */
  private void skipVarUInteger(CellSliceLazy cs, int n) {
    int lenBits = 32 - Integer.numberOfLeadingZeros(n - 1);
    int len = cs.loadUint(lenBits).intValue();
    cs.skipBits(len * 8);
  }
}
