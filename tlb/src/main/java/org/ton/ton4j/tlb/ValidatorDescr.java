package org.ton.ton4j.tlb;

import java.math.BigInteger;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * validator#53 public_key:SigPubKey weight:uint64 = ValidatorDescr;
 * validator_addr#73 public_key:SigPubKey weight:uint64 adnl_addr:bits256 = ValidatorDescr;
 * </pre>
 */
public interface ValidatorDescr {

  Cell toCell();

  static ValidatorDescr deserialize(CellSlice cs) {
    int magic = cs.preloadUint(8).intValue();
    if (magic == 0x53) {
      return Validator.deserialize(cs);
    } else if (magic == 0x73) {
      return ValidatorAddr.deserialize(cs);
    } else {
      throw new Error("Cannot deserialize ValidatorDescr, magic: " + magic);
    }
  }

  String getPublicKeyHex();

  String getAdnlAddressHex();

  BigInteger getWeight();
}
