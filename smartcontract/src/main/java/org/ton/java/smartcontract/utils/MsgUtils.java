package org.ton.java.smartcontract.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.util.List;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.tlb.*;
import org.ton.java.tonlib.types.ExtraCurrency;
import org.ton.java.utils.Utils;

public class MsgUtils {

  public static Message createExternalMessage(Address destination, StateInit stateInit, Cell body) {

    return Message.builder()
        .info(
            ExternalMessageInInfo.builder()
                .dstAddr(
                    MsgAddressIntStd.builder()
                        .workchainId(destination.wc)
                        .address(destination.toBigInteger())
                        .build())
                .build())
        .init(nonNull(stateInit) ? stateInit : null)
        .body(nonNull(body) ? body : null)
        .build();
  }

  public static Message createExternalMessageWithSignedBody(
      TweetNaclFast.Signature.KeyPair keyPair,
      Address destination,
      StateInit stateInit,
      Cell body) {
    Message externalMessage =
        Message.builder()
            .info(
                ExternalMessageInInfo.builder()
                    .dstAddr(
                        MsgAddressIntStd.builder()
                            .workchainId(destination.wc)
                            .address(destination.toBigInteger())
                            .build())
                    .build())
            .init(nonNull(stateInit) ? stateInit : null)
            .build();

    if (isNull(body)) {
      body = CellBuilder.beginCell().endCell();
    }
    externalMessage.setBody(
        CellBuilder.beginCell()
            .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
            .storeCell(body)
            .endCell());

    return externalMessage;
  }

  public static Message createInternalMessage(
      Address destination, BigInteger amount, StateInit stateInit, Cell body, Boolean bounce) {
    return Message.builder()
        .info(
            InternalMessageInfo.builder()
                .bounce(bounce)
                .dstAddr(
                    MsgAddressIntStd.builder()
                        .workchainId(destination.wc)
                        .address(destination.toBigInteger())
                        .build())
                .value(CurrencyCollection.builder().coins(amount).build())
                .build())
        .init(nonNull(stateInit) ? stateInit : null)
        .body(nonNull(body) ? body : null)
        .build();
  }

  public static Message createInternalMessage(
      Address destination,
      BigInteger amount,
      List<ExtraCurrency> extraCurrencies,
      StateInit stateInit,
      Cell body,
      Boolean bounce) {
    return Message.builder()
        .info(
            InternalMessageInfo.builder()
                .bounce(bounce)
                .dstAddr(
                    MsgAddressIntStd.builder()
                        .workchainId(destination.wc)
                        .address(destination.toBigInteger())
                        .build())
                .value(
                    CurrencyCollection.builder()
                        .coins(amount)
                        .extraCurrencies(convertExtraCurrenciesToHashMap(extraCurrencies))
                        .build())
                .build())
        .init(nonNull(stateInit) ? stateInit : null)
        .body(nonNull(body) ? body : null)
        .build();
  }

  public static Message createInternalMessageWithSourceAddress(
      Address source,
      Address destination,
      BigInteger amount,
      StateInit stateInit,
      Cell body,
      Boolean bounce) {
    return Message.builder()
        .info(
            InternalMessageInfo.builder()
                .bounce(bounce)
                .srcAddr(
                    MsgAddressIntStd.builder()
                        .workchainId(source.wc)
                        .address(source.toBigInteger())
                        .build())
                .dstAddr(
                    MsgAddressIntStd.builder()
                        .workchainId(destination.wc)
                        .address(destination.toBigInteger())
                        .build())
                .value(CurrencyCollection.builder().coins(amount).build())
                .build())
        .init(nonNull(stateInit) ? stateInit : null)
        .body(nonNull(body) ? body : null)
        .build();
  }

  public static Message createInternalMessageWithSourceAddress(
      Address source,
      Address destination,
      BigInteger amount,
      List<ExtraCurrency> extraCurrencies,
      StateInit stateInit,
      Cell body,
      Boolean bounce) {
    return Message.builder()
        .info(
            InternalMessageInfo.builder()
                .bounce(bounce)
                .srcAddr(
                    MsgAddressIntStd.builder()
                        .workchainId(source.wc)
                        .address(source.toBigInteger())
                        .build())
                .dstAddr(
                    MsgAddressIntStd.builder()
                        .workchainId(destination.wc)
                        .address(destination.toBigInteger())
                        .build())
                .value(
                    CurrencyCollection.builder()
                        .coins(amount)
                        .extraCurrencies(convertExtraCurrenciesToHashMap(extraCurrencies))
                        .build())
                .build())
        .init(nonNull(stateInit) ? stateInit : null)
        .body(nonNull(body) ? body : null)
        .build();
  }

  public static Cell createTextMessageBody(String text) {
    return CellBuilder.beginCell().storeUint(0, 32).storeSnakeString(text).endCell();
  }

  public static TonHashMapE convertExtraCurrenciesToHashMap(List<ExtraCurrency> extraCurrencies) {

    if (isNull(extraCurrencies)) {
      return null;
    }
    TonHashMapE x = new TonHashMapE(32);

    for (ExtraCurrency ec : extraCurrencies) {
      x.elements.put(ec.getId(), ec.getAmount());
    }
    return x;
  }
}
