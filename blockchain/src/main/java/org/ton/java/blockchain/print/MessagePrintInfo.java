package org.ton.java.blockchain.print;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;

@Builder
@Data
@Slf4j
public class MessagePrintInfo {
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
    String comment;

    private static String header =
            "| in/out | type         | op        | value           | fwdFee          | ihrFee          | importFee       | timestamp           | lt             | src             | dst             | comment                    |";

    public void printMessageInfo() {
        MessagePrintInfo msgFee = this;

        String str =
                String.format(
                        "| %-7s| %-13s| %-10s| %-16s| %-16s| %-16s| %-16s| %-20s| %-15s| %-16s| %-16s| %-27s|",
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
                        getShortSourceAddress(),
                        getShortDestinationAddress(),
                        msgFee.getComment()
                );
        log.info(str);
    }

    public static void printMessageInfoHeader() {
        log.info("");
        log.info("Messages");
        log.info(StringUtils.repeat("-", header.length()));
        log.info(header);
        log.info(StringUtils.repeat("-", header.length()));
    }

    public static void printMessageInfoFooter() {
        log.info(StringUtils.repeat("-", header.length()));
    }

    public String getShortSourceAddress() {
        if (StringUtils.isNotEmpty(src)) {
            return src.substring(0, 7) + "..." + src.substring(src.length() - 6, src.length() - 1);
        } else {
            return "N/A";
        }
    }

    public String getShortDestinationAddress() {
        if (StringUtils.isNotEmpty(dst)) {
            return dst.substring(0, 7) + "..." + dst.substring(dst.length() - 6, dst.length() - 1);
        } else {
            return "N/A";
        }
    }
}
