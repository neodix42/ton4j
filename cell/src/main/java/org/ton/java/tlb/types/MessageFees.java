package org.ton.java.tlb.types;

import static java.util.Objects.isNull;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.utils.Utils;

@Builder
@Data
public class MessageFees {
  String direction;
  String type;
  String op;
  BigInteger fwdFee;
  BigInteger value;
  BigInteger ihrFee;
  BigInteger createdAt;
  BigInteger createdLt;
  BigInteger importFee;

  public void printMessageFees() {
    MessageFees msgFee = this;

    String str =
        String.format(
            "| %-8s| %-25s| %-9s| %-16s| %-16s| %-16s| %-16s| %-20s| %-8s|",
            msgFee.getDirection(),
            msgFee.getType(),
            msgFee.getOp(),
            isNull(msgFee.getValue()) ? "N/A" : Utils.formatNanoValueZero(msgFee.getValue()),
            isNull(msgFee.getFwdFee()) ? "N/A" : Utils.formatNanoValueZero(msgFee.getFwdFee()),
            isNull(msgFee.getIhrFee()) ? "N/A" : Utils.formatNanoValueZero(msgFee.getIhrFee()),
            isNull(msgFee.getImportFee())
                ? "N/A"
                : Utils.formatNanoValueZero(msgFee.getImportFee()),
            isNull(msgFee.getCreatedAt()) ? "N/A" : Utils.toUTC(msgFee.getCreatedAt().longValue()),
            isNull(msgFee.getCreatedLt()) ? "N/A" : msgFee.getCreatedLt().toString());
    System.out.println(str);
  }

  public static void printMessageFeesHeader() {
    String header =
        "| in/out? | type                     | op       | value           | fwdFee          | ihrFee          | importFee       | timestamp           | lt      |";
    System.out.println("Messages");
    System.out.println(
        "---------------------------------------------------------------------------------------------------------------------------------------------------------");
    System.out.println(header);
    System.out.println(
        "---------------------------------------------------------------------------------------------------------------------------------------------------------");
  }

  public static void printMessageFeesFooter() {
    System.out.println(
        "---------------------------------------------------------------------------------------------------------------------------------------------------------");
  }
}
