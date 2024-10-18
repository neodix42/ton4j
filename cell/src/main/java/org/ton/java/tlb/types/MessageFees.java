package org.ton.java.tlb.types;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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
  String src;
  String dst;

  public void printMessageFees() {
    MessageFees msgFee = this;

    String str =
        String.format(
            "| %-7s| %-13s| %-9s| %-16s| %-16s| %-16s| %-16s| %-20s| %-15s| %-16s| %-14s |",
            msgFee.getDirection(),
            msgFee.getType(),
            msgFee.getOp(),
            isNull(msgFee.getValue()) ? "N/A" : Utils.formatNanoValueZero(msgFee.getValue()),
            isNull(msgFee.getFwdFee()) ? "N/A" : Utils.formatNanoValueZero(msgFee.getFwdFee()),
            isNull(msgFee.getIhrFee()) ? "N/A" : Utils.formatNanoValueZero(msgFee.getIhrFee()),
            isNull(msgFee.getImportFee())
                ? "N/A"
                : Utils.formatNanoValueZero(msgFee.getImportFee()),
            isNull(msgFee.getCreatedAt())
                ? "N/A"
                : (msgFee.getCreatedAt().longValue() == 0)
                    ? "0"
                    : Utils.toUTC(msgFee.getCreatedAt().longValue()),
            isNull(msgFee.getCreatedLt()) ? "N/A" : msgFee.getCreatedLt().toString(),
            getSrc(),
            getDst());
    System.out.println(str);
  }

  public static void printMessageFeesHeader() {
    String header =
        "| in/out | type         | op       | value           | fwdFee          | ihrFee          | importFee       | timestamp           | lt             | src             | dst             |";
    System.out.println("Messages");
    System.out.println(
        "---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
    System.out.println(header);
    System.out.println(
        "---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
  }

  public static void printMessageFeesFooter() {
    System.out.println(
        "---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
  }

  public String getSrc() {
    if (nonNull(src)) {
      return src.substring(0, 7) + "..." + src.substring(src.length() - 6, src.length() - 1);
    } else {
      return "N/A";
    }
  }

  public String getDst() {
    if (nonNull(dst)) {
      return dst.substring(0, 7) + "..." + dst.substring(dst.length() - 6, dst.length() - 1);
    } else {
      return "N/A";
    }
  }
}
