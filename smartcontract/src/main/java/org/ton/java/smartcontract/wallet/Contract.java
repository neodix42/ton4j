package org.ton.java.smartcontract.wallet;


import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletConfig;
import org.ton.java.smartcontract.wallet.v1.WalletV1ContractR1;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Interface for all smart contract objects in ton4j.
 */
public interface Contract<T extends WalletConfig> {

    Options getOptions();

    String getName();

    default Address getAddress() {
        return createStateInit().getAddress();
    }

    default MsgAddressIntStd getAddressIntStd() {
        Address ownAddress = createStateInit().getAddress();
        return MsgAddressIntStd.builder()
                .workchainId(ownAddress.wc)
                .address(ownAddress.toBigInteger())
                .build();
    }

    /**
     * @return Cell containing contact code
     */
    default Cell createCodeCell() {
        if (isNull(getOptions().code)) {
            throw new Error("Contract: options.code is not defined");
        }
        return getOptions().code;
    }

    /**
     * Method to override
     *
     * @return {Cell} cell contains contract data
     */

    Cell createDataCell();

    /**
     * Message StateInit consists of initial contract code, data and address in a blockchain
     *
     * @return StateInit
     */
    default StateInit createStateInit() {
        return StateInit.builder()
                .code(createCodeCell())
                .data(createDataCell())
                .build();
    }

    Cell createTransferBody(T config);

    ExtMessageInfo deploy(Tonlib tonlib, T config);

    default long getSeqno(Tonlib tonlib) {

        if (this instanceof WalletV1ContractR1) {
            throw new Error("Wallet V1R1 does not have seqno method");
        }

        return tonlib.getSeqno(getAddress());
    }

    default Message createExternalMessage(Address destination, boolean stateInit, Cell body) {
        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(destination.wc)
                                .address(destination.toBigInteger())
                                .build())
                        .build()).build();
        if (stateInit) {
            externalMessage.setInit(createStateInit());
        }
        if (isNull(body)) {
            body = CellBuilder.beginCell().endCell();
        }
        externalMessage.setBody(CellBuilder.beginCell()
                .storeBytes(Utils.signData(getOptions().getPublicKey(), getOptions().getSecretKey(), body.hash()))
                .storeCell(body) // was ref careful
                .endCell());

        return externalMessage;
    }

    default Message createInternalMessage(Address destination, BigInteger amount, Cell body) {
        Message internalMessage = Message.builder()
                .info(InternalMessageInfo.builder()
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(destination.wc)
                                .address(destination.toBigInteger())
                                .build())
                        .value(CurrencyCollection.builder().coins(amount).build())
                        .build()).build();

        if (nonNull(body)) {
            internalMessage.setBody(body);
        }

        return internalMessage;
    }

    /**
     * crates external with internal msg as body
     */
    default Message createTransferMessage(T config) {

//        Cell body = createTransferBody(config);
//        Cell intMsg = this.createInternalMessage(config.destination, config.amount, body).toCell();
//
//        return this.createExternalMessage(config.destination, false, intMsg);
        return null;
    }
}