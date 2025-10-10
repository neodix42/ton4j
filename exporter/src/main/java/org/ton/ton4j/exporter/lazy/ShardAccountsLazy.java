package org.ton.ton4j.exporter.lazy;

import java.math.BigInteger;
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

  /**
   * Lookup a single account by address using Patricia tree traversal Port of C++
   * DictionaryFixed::lookup from crypto/vm/dict.cpp
   */
  public ShardAccountLazy getShardAccountByAddress(Address address) {
    if (rootSlice == null) {
      return null;
    }

    // HashmapAugE structure: first bit indicates if empty (0) or non-empty (1)
    // ahme_empty$0 {n:#} {X:Type} {Y:Type} extra:Y = HashmapAugE n X Y;
    // ahme_root$1 {n:#} {X:Type} {Y:Type} root:^(HashmapAug n X Y) extra:Y = HashmapAugE n X Y;
    boolean nonEmpty = rootSlice.loadBit();

    if (!nonEmpty) {
      // Empty hashmap
      return null;
    }

    // Load the actual root cell from the reference
    if (rootSlice.getRefsCountLazy() < 1) {
      log.error("HashmapAugE marked as non-empty but has no root reference");
      return null;
    }

    // find by hash and load non-empty dictAugE
    byte[] rootHash = Utils.slice(rootSlice.hashes, 0, 32);
    Cell rootCell = rootSlice.getRefByHash(rootHash);

    // Prepare the key
    BigInteger key = address.toBigInteger();
    BitString keyBits = new BitString(256);
    keyBits.writeUint(key, 256);
    keyBits.readCursor = 0;

    // Traverse the Patricia tree - following C++ DictionaryFixed::lookup
    int n = 256; // remaining key bits
    int keyOffset = 0; // current position in key
    while (true) {
      // Parse label using LabelParser (like C++)
      LabelParser label =
          new LabelParser(rootSlice.cellDbReader, rootCell, n, 0); // label_mode=0 for no validation

      // Check if label is a prefix of remaining key
      // Create a view of the remaining key bits starting from keyOffset
      BitString remainingKey = new BitString(n);
      for (int i = 0; i < n && keyOffset + i < keyBits.getUsedBits(); i++) {
        remainingKey.writeBit(keyBits.get(keyOffset + i));
      }

      if (!label.isPrefixOf(remainingKey, n)) {
        log.error("not a prefix");
        return null;
      }

      n -= label.getLBits();

      if (n <= 0) {
        // Reached a leaf node
        assert n == 0;
        label.skipLabel();
        CellSliceLazy leafSlice = label.getRemainder();
        // read extra (DepthBalanceInfo)
        DepthBalanceInfoLazy depthBalanceInfoLazy = DepthBalanceInfoLazy.deserialize(leafSlice);
        // read value (ShardAccount)
        return ShardAccountLazy.deserialize(leafSlice);
      }

      keyOffset += label.getLBits();

      // Not at leaf, need to follow a branch, read next key bit to determine branch
      boolean sw = keyBits.get(keyOffset);
      keyOffset++;
      n++;

      byte[] hash = Utils.slice(label.getRemainder().hashes, (sw ? 1 : 0) * 32, 32);
      rootCell = label.getRemainder().getRefByHash(hash);
    }
  }
}
