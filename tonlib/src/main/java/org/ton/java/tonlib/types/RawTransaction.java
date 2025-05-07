package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.Transaction;

@Builder
@Data
public class RawTransaction implements Serializable {
  @SerializedName("@type")
  final String type = "raw.transaction";

  long utime;
  String data;
  LastTransactionId transaction_id;
  String fee;
  String storage_fee;
  String other_fee;
  RawMessage in_msg;
  List<RawMessage> out_msgs;

  public List<ExtraCurrency> getExtraCurrencies() {
    return this.getIn_msg().getExtra_currencies();
  }

  /**
   * @return first extra-currency id from the list of extra-currencies. Returns Long.MIN_VALUE if
   *     list is empty;
   */
  public long getFirstExtraCurrencyId() {
    for (ExtraCurrency ec : this.getIn_msg().getExtra_currencies()) {
      return ec.getId();
    }
    return Long.MIN_VALUE;
  }

  /**
   * @return first extra-currency amount from the list of extra-currencies. Returns null if list is
   *     empty;
   */
  public BigInteger getFirstExtraCurrencyValue() {
    for (ExtraCurrency ec : this.getIn_msg().getExtra_currencies()) {
      return ec.getAmount();
    }
    return null;
  }

  public Transaction getTransactionAsTlb() {
    return Transaction.deserialize(
        CellSlice.beginParse(CellBuilder.beginCell().fromBocBase64(getData()).endCell()));
  }
}
