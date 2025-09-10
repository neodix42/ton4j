package org.ton.ton4j.tlb;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.*;

/**
 *
 *
 * <pre> *
 * _ (HashmapE 96 ProcessedUpto) = ProcessedInfo;
 * </pre>
 */
@Builder
@Data
public class ProcessedInfo {
  TonHashMapE processedInfo;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeCell(
            processedInfo.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 96).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((ProcessedUpto) v).toCell()).endCell()))
        .endCell();
  }

  public static ProcessedInfo deserialize(CellSlice cs) {
    return ProcessedInfo.builder()
        .processedInfo(
            cs.loadDictE(
                96, k -> k.readUint(96), v -> ProcessedUpto.deserialize(CellSlice.beginParse(v))))
        .build();
  }

  public List<ProcessedUpto> getProcessedInfoAsList() {
    List<ProcessedUpto> processedInfo = new ArrayList<>();
    for (Map.Entry<Object, Object> entry : this.processedInfo.elements.entrySet()) {
      processedInfo.add((ProcessedUpto) entry.getValue());
    }
    return processedInfo;
  }
}
