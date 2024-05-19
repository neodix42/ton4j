package org.ton.java.smartcontract.utils;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.tlb.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class MsgUtils {

    public static Message createExternalMessageWithSignedBody(TweetNaclFast.Signature.KeyPair keyPair,
                                                              Address destination, StateInit stateInit, Cell body) {
        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(MsgAddressIntStd.builder()
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

    public static Message createInternalMessage(Address destination,
                                                BigInteger amount,
                                                StateInit stateInit,
                                                Cell body) {
        return Message.builder()
                .info(InternalMessageInfo.builder()
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(destination.wc)
                                .address(destination.toBigInteger())
                                .build())
                        .value(CurrencyCollection.builder().coins(amount).build())
                        .build())
                .init(nonNull(stateInit) ? stateInit : null)
                .body(nonNull(body) ? body : null)
                .build();
    }

    public static Message createNonBounceableInternalMessage(Address destination,
                                                             BigInteger amount,
                                                             StateInit stateInit,
                                                             Cell body) {
        return Message.builder()
                .info(InternalMessageInfo.builder()
                        .bounce(false)
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(destination.wc)
                                .address(destination.toBigInteger())
                                .build())
                        .value(CurrencyCollection.builder().coins(amount).build())
                        .build())
                .init(nonNull(stateInit) ? stateInit : null)
                .body(nonNull(body) ? body : null)
                .build();
    }
}
