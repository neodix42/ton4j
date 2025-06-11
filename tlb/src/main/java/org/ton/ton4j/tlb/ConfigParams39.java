package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;

/**
 *
 *
 * <pre>
 * validator_temp_key#3
 * adnl_addr:bits256
 * temp_public_key:SigPubKey
 * seqno:# valid_until:uint32 =
 * ValidatorTempKey;
 * signed_temp_key#4
 * key:^ValidatorTempKey
 * signature:CryptoSignature = ValidatorSignedTempKey;
 * _ (HashmapE 256 ValidatorSignedTempKey) = ConfigParam 39;
 * </pre>
 */
@Builder
@Data
public class ConfigParams39 implements Serializable {
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
                k -> k.readUint(16),
                v -> ValidatorSignedTempKey.deserialize(CellSlice.beginParse(v))))
        .build();
  }
}
