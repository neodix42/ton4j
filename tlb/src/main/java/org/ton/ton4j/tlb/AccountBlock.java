package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapAug;

/**
 *
 *
 * <pre>
 * acc_trans#5
 *  account_addr:bits256
 *  transactions:(HashmapAug 64 ^Transaction CurrencyCollection)
 *  state_update:^(HASH_UPDATE Account)
 *  = AccountBlock;
 *  </pre>
 */
@Builder
@Data
public class AccountBlock implements Serializable {
  long magic;
  BigInteger addr;
  TonHashMapAug transactions;
  Cell stateUpdate; // todo deserialize

  public Cell toCell() {
    Cell dictCell =
        transactions.serialize(
            k -> CellBuilder.beginCell().storeUint((BigInteger) k, 64).endCell().getBits(),
            v -> CellBuilder.beginCell().storeRef(((Transaction) v).toCell()),
            e -> CellBuilder.beginCell().storeCell(((CurrencyCollection) e).toCell()),
            (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1) // todo
            );
    return CellBuilder.beginCell()
        .storeUint(0x5, 4)
        .storeUint(addr, 256)
        .storeDict(dictCell)
        .storeRef(stateUpdate)
        .endCell();
  }

  public static AccountBlock deserialize(CellSlice cs) {
    long magic = cs.loadUint(4).longValue();
    assert (magic == 0x5L)
        : "AccountBlock: magic not equal to 0x5, found 0x" + Long.toHexString(magic);

    return AccountBlock.builder()
        .magic(0x5)
        .addr(cs.loadUint(256))
        .transactions(
            cs.loadDictAug(
                64,
                k -> k.readUint(64),
                v ->
                    (v.getRefsCount() > 0)
                        ? Transaction.deserialize(CellSlice.beginParse(v.loadRef()))
                        : null,
                e -> {
                  return CurrencyCollection.deserialize(e);
                }))
        .stateUpdate(cs.loadRef()) // ^(HASH_UPDATE Account) todo
        .build();
  }
}
