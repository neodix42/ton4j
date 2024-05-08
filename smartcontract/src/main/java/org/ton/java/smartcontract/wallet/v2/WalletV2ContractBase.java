package org.ton.java.smartcontract.wallet.v2;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletV2Config;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tlb.types.ExternalMessageInfo;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.Date;

import static java.util.Objects.nonNull;

public class WalletV2ContractBase implements Contract<WalletV2Config> {
    Options options;
    Address address;

    protected WalletV2ContractBase(Options options) {
        this.options = options;
    }

    @Override
    public String getName() {
        return "override me";
    }

    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(0, 32); // seqno
        cell.storeBytes(getOptions().publicKey);
        return cell.endCell();
    }

    public Cell createDeployMessage(WalletV2Config config) {
        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(0, 32);
        for (int i = 0; i < 32; i++) { // valid-until
            message.storeBit(true);
        }
        return message.endCell();
    }

    /**
     * Creates message payload with seqno and validUntil fields
     */
    @Override
    public Cell createTransferBody(WalletV2Config config) {

        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(BigInteger.valueOf(config.getSeqno()), 32);

        Date date = new Date();
        long timestamp = (long) Math.floor(date.getTime() / 1e3);
        message.storeUint(BigInteger.valueOf(timestamp + 60L), 32);

        if (nonNull(config.getDestination1())) {
            Message order = this.createInternalMessage(config.getDestination1(), config.getAmount1(), null, null);
            message.storeUint(3 & 0xff, 8);
            message.storeRef(order.toCell());
        }
        if (nonNull(config.getDestination2())) {
            Message order = this.createInternalMessage(config.getDestination2(), config.getAmount2(), null, null);
            message.storeUint(3 & 0xff, 8);
            message.storeRef(order.toCell());
        }
        if (nonNull(config.getDestination3())) {
            Message order = this.createInternalMessage(config.getDestination3(), config.getAmount3(), null, null);
            message.storeUint(3 & 0xff, 8);
            message.storeRef(order.toCell());
        }
        if (nonNull(config.getDestination4())) {
            Message order = this.createInternalMessage(config.getDestination4(), config.getAmount3(), null, null);
            message.storeUint(3 & 0xff, 8);
            message.storeRef(order.toCell());
        }

        return message.endCell();
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public ExtMessageInfo deploy(Tonlib tonlib, WalletV2Config config) {
        Cell body = createDeployMessage(config);

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .init(createStateInit())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(getOptions().getPublicKey(), getOptions().getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }


    public ExtMessageInfo sendTonCoins(Tonlib tonlib, WalletV2Config config) {
        Cell body = createTransferBody(config);

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(getOptions().getPublicKey(), getOptions().getSecretKey(), body.hash()))
                        .storeCell(body) // was storeRef!!
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }


    /*
     * Sends amount of nano toncoins to destination addresses using auto-fetched seqno with the body and default send-mode 3
     * Sends amount of nano toncoins to destination address using auto-fetched seqno with the body and default send-mode 3
     * Sends amount of nano toncoins to destination address using auto-fetched seqno with the body and default send-mode 3 with body
     * Sends amount of nano toncoins to destination address using auto-fetched seqno with the body and default send-mode 3
     * Sends amount of nano toncoins to destination addresses using auto-fetched seqno without the body and with the comment and default send-mode 3
     * Sends amount of nano toncoins to destination addresses using auto-fetched seqno with the body and default send-mode 3


    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress1, Address destinationAddress2, BigInteger amount, Cell body) {
        long seqno = getSeqno(tonlib);

        Cell orderHeader1 = Contract.createInternalMessageHeader(destinationAddress1, amount);
        Cell order1 = Contract.createCommonMsgInfo(orderHeader1, null, body);

        Cell orderHeader2 = Contract.createInternalMessageHeader(destinationAddress2, amount);
        Cell order2 = Contract.createCommonMsgInfo(orderHeader2, null, body);

        Cell signingMessageAll = createSigningMessage(seqno)
                .storeUint(3 & 0xff, 8)
                .storeUint(3 & 0xff, 8)
                .storeRef(order1)
                .storeRef(order2)
                .endCell();

        ExternalMessage msg = createExternalMessage(signingMessageAll, secretKey, seqno);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }


    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress1, Address destinationAddress2, BigInteger amount, String comment) {
        return sendTonCoins(tonlib, secretKey, destinationAddress1, destinationAddress2, amount, CellBuilder.beginCell().storeUint(0, 32).storeString(comment).endCell());
    }


    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress1, Address destinationAddress2, Address destinationAddress3, BigInteger amount, Cell body) {
        long seqno = getSeqno(tonlib);

        Cell orderHeader1 = Contract.createInternalMessageHeader(destinationAddress1, amount);
        Cell order1 = Contract.createCommonMsgInfo(orderHeader1, null, body);

        Cell orderHeader2 = Contract.createInternalMessageHeader(destinationAddress2, amount);
        Cell order2 = Contract.createCommonMsgInfo(orderHeader2, null, body);

        Cell orderHeader3 = Contract.createInternalMessageHeader(destinationAddress3, amount);
        Cell order3 = Contract.createCommonMsgInfo(orderHeader3, null, body);

        Cell signingMessageAll = createSigningMessage(seqno)
                .storeUint(3 & 0xff, 8)
                .storeUint(3 & 0xff, 8)
                .storeUint(3 & 0xff, 8)
                .storeRef(order1)
                .storeRef(order2)
                .storeRef(order3)
                .endCell();

        ExternalMessage msg = createExternalMessage(signingMessageAll, secretKey, seqno);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress1, Address destinationAddress2, Address destinationAddress3, BigInteger amount, String comment) {
        return sendTonCoins(tonlib, secretKey, destinationAddress1, destinationAddress2, destinationAddress3, amount, CellBuilder.beginCell().storeUint(0, 32).storeString(comment).endCell());
    }

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress1, Address destinationAddress2, Address destinationAddress3, Address destinationAddress4, BigInteger amount, Cell body) {
        long seqno = getSeqno(tonlib);

        Cell orderHeader1 = Contract.createInternalMessageHeader(destinationAddress1, amount);
        Cell order1 = Contract.createCommonMsgInfo(orderHeader1, null, body);

        Cell orderHeader2 = Contract.createInternalMessageHeader(destinationAddress2, amount);
        Cell order2 = Contract.createCommonMsgInfo(orderHeader2, null, body);

        Cell orderHeader3 = Contract.createInternalMessageHeader(destinationAddress3, amount);
        Cell order3 = Contract.createCommonMsgInfo(orderHeader3, null, body);

        Cell orderHeader4 = Contract.createInternalMessageHeader(destinationAddress4, amount);
        Cell order4 = Contract.createCommonMsgInfo(orderHeader4, null, body);

        Cell signingMessageAll = createSigningMessage(seqno)
                .storeUint(3 & 0xff, 8)
                .storeUint(3 & 0xff, 8)
                .storeUint(3 & 0xff, 8)
                .storeUint(3 & 0xff, 8)
                .storeRef(order1)
                .storeRef(order2)
                .storeRef(order3)
                .storeRef(order4)
                .endCell();

        ExternalMessage msg = createExternalMessage(signingMessageAll, secretKey, seqno);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }
    */
}
