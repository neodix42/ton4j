package org.ton.java.tlb;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

@Builder
@Data
/**
 * validator_temp_key#3 adnl_addr:bits256 temp_public_key:SigPubKey seqno:# valid_until:uint32 =
 * ValidatorTempKey;
 *
 * <p>signed_temp_key#4 key:^ValidatorTempKey signature:CryptoSignature = ValidatorSignedTempKey;
 *
 * <p>_ (HashmapE 256 ValidatorSignedTempKey) = ConfigParam 39;
 */
public class ConfigParams39 {
  TonHashMapE validatorSignedTemp;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeDict(
            validatorSignedTemp.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
                v ->
                    CellBuilder.beginCell()
                        .storeCell(((ValidatorSignedTempKey) v).toCell())
                        .endCell()))
        .endCell();
  }

  public static ConfigParams39 deserialize(CellSlice cs) {
    return ConfigParams39.builder()
        .validatorSignedTemp(
            cs.loadDictE(
                256,
                k -> k.readInt(16),
                v -> ValidatorSignedTempKey.deserialize(CellSlice.beginParse(v))))
        .build();
  }
}
