package org.ton.ton4j.smartcontract.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.util.List;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.TonHashMapE;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tonlib.types.ExtraCurrency;
import org.ton.ton4j.utils.Utils;

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

  /**
   * creates external message where body hash signed externally
   *
   * @param signedBodyHash signed body hash
   * @param destination destination address
   * @param stateInit state init
   * @param body message body
   * @return Message
   */
  public static Message createExternalMessageWithSignedBody(
      byte[] signedBodyHash, Address destination, StateInit stateInit, Cell body) {
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
        CellBuilder.beginCell().storeBytes(signedBodyHash).storeCell(body).endCell());

    return externalMessage;
  }

  /** without source address */
  public static MessageRelaxed createInternalMessageRelaxed(
      Address destination,
      BigInteger amount,
      List<ExtraCurrency> extraCurrencies,
      StateInit stateInit,
      Cell body,
      Boolean bounce) {
    return MessageRelaxed.builder()
        .info(
            InternalMessageInfoRelaxed.builder()
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

  public static Message createInternalMessage(
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
