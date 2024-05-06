package org.ton.java.smartcontract.integrationtests;


import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.CustomContractConfig;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tlb.types.ExternalMessageInfo;
import org.ton.java.tlb.types.Message;
import org.ton.java.tlb.types.MsgAddressExtNone;
import org.ton.java.tlb.types.MsgAddressIntStd;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.Date;

public class CustomContract implements Contract<CustomContractConfig> {
    Options options;
    Address address;

    public CustomContract(Options options) {
        this.options = options;
        options.code = CellBuilder.beginCell().fromBoc("B5EE9C7241010C0100B2000114FF00F4A413F4BCF2C80B01020120020302014804050094F28308D71820D31FD31FD33F02F823BBF263ED44D0D31FD3FFD33F305152BAF2A105F901541065F910F2A2F800019320D74A96D307D402FB00E8D103A4C8CB1F12CBFFCB3FCB3FC9ED540004D03002012006070201200809001DBDC3676A268698F98E9FF98EB859FC0017BB39CED44D0D31F31D70BFF80202710A0B0022AA77ED44D0D31F31D3FF31D33F31D70B3F0010A897ED44D0D70B1F56A9826C").endCell();
    }

    @Override
    public String getName() {
        return "customContract";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public Cell createDataCell() {
        System.out.println("CustomContract createDataCell");
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(0, 32); // seqno
        cell.storeBytes(getOptions().publicKey); // 256 bits
        cell.storeUint(2, 64); // stored_x_data
        return cell.endCell();
    }

//    @Override
//    public Cell createSigningMessage(long seqno) {
//        return createSigningMessage(seqno, 4L);
//    }

    @Override
    public Cell createTransferBody(CustomContractConfig config) {
        System.out.println("CustomContract createSigningMessage");

        CellBuilder message = CellBuilder.beginCell();

        message.storeUint(BigInteger.valueOf(config.getSeqno()), 32); // seqno

        if (config.getSeqno() == 0) {
            for (int i = 0; i < 32; i++) {
                message.storeBit(true);
            }
        } else {
            Date date = new Date();
            long timestamp = (long) Math.floor(date.getTime() / 1e3);
            message.storeUint(BigInteger.valueOf(timestamp + 60L), 32);
        }

        message.storeUint(BigInteger.valueOf(config.getExtraField()), 64); // extraField
        return message.endCell();
    }

    @Override
    public ExtMessageInfo deploy(Tonlib tonlib, CustomContractConfig config) {
        Address ownAddress = getAddress();

        Cell body = createTransferBody(config);

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .srcAddr(MsgAddressExtNone.builder().build())
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(ownAddress.wc)
                                .address(ownAddress.toBigInteger())
                                .build())
                        .build())
                .init(createStateInit())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(getOptions().getPublicKey(), options.getSecretKey(), body.hash()))
                        .storeRef(body)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, CustomContractConfig config) {
        Cell body = createTransferBody(config);
        Address ownAddress = getAddress();
        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .srcAddr(MsgAddressExtNone.builder().build())
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(ownAddress.wc)
                                .address(ownAddress.toBigInteger())
                                .build())
                        .build())
                .init(null)
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(getOptions().getPublicKey(), options.getSecretKey(), body.hash()))
                        .storeRef(body)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }

}
