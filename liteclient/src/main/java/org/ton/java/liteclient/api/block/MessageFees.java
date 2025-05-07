package org.ton.java.liteclient.api.block;

import static java.util.Objects.isNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Builder
@Data
@Slf4j
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
            "| %-7s| %-13s| %-9s| %-16s| %-16s| %-16s| %-16s| %-20s| %-15s| %-68s| %-67s |",
            msgFee.getDirection(),
            msgFee.getType(),
            msgFee.getOp(),
            isNull(msgFee.getValue()) ? "N/A" : formatNanoValueZero(msgFee.getValue()),
            isNull(msgFee.getFwdFee()) ? "N/A" : formatNanoValueZero(msgFee.getFwdFee()),
            isNull(msgFee.getIhrFee()) ? "N/A" : formatNanoValueZero(msgFee.getIhrFee()),
            isNull(msgFee.getImportFee()) ? "N/A" : formatNanoValueZero(msgFee.getImportFee()),
            isNull(msgFee.getCreatedAt())
                ? "N/A"
                : (msgFee.getCreatedAt().longValue() == 0)
                    ? "0"
                    : toUTC(msgFee.getCreatedAt().longValue()),
            isNull(msgFee.getCreatedLt()) ? "N/A" : msgFee.getCreatedLt().toString(),
            getSrc(),
            getDst());
    log.info(str);
  }

  public String toUTC(long timestamp) {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .format(LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC));
  }

  public String formatNanoValueZero(BigInteger nanoCoins) {
    if (isNull(nanoCoins)) {
      return "N/A";
    }
    if (nanoCoins.compareTo(BigInteger.ZERO) == 0) {
      return "0";
    } else {
      return String.format(
          "%,.9f",
          new BigDecimal(nanoCoins)
              .divide(BigDecimal.valueOf(1_000_000_000), 9, RoundingMode.HALF_UP));
    }
  }

  public static void printMessageFeesHeader() {
    String header =
        "| in/out | type         | op       | value           | fwdFee          | ihrFee          | importFee       | timestamp           | lt             | source                                                              | destination                                                         |";
    log.info("");
    log.info("Messages");
    log.info(
        "----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
    log.info(header);
    log.info(
        "----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
  }

  public static void printMessageFeesFooter() {
    log.info(
        "----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
  }

  public String getSrc() {
    if (StringUtils.isNotEmpty(src)) {
      return src.substring(0, 7) + "..." + src.substring(src.length() - 6, src.length() - 1);
    } else {
      return "N/A";
    }
  }

  public String getDst() {
    if (StringUtils.isNotEmpty(dst)) {
      return dst.substring(0, 7) + "..." + dst.substring(dst.length() - 6, dst.length() - 1);

    } else {
      return "N/A";
    }
  }
}
