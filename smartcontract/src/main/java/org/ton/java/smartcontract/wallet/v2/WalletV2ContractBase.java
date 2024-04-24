package org.ton.java.smartcontract.wallet.v2;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;

import java.math.BigInteger;
import java.util.Date;

import static java.util.Objects.isNull;

public class WalletV2ContractBase implements WalletContract {
    Options options;
    Address address;

    protected WalletV2ContractBase(Options options) {
        this.options = options;
    }

    @Override
    public String getName() {
        return "override me";
    }

    /**
     * Creates message payload with seqno and validUntil fields
     *
     * @param seqno long
     * @return Cell
     */
    @Override
    public CellBuilder createSigningMessage(long seqno) {

        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(BigInteger.valueOf(seqno), 32);
        if (seqno == 0) {
            for (int i = 0; i < 32; i++) {
                message.storeBit(true);
            }
        } else {
            Date date = new Date();
            long timestamp = (long) Math.floor(date.getTime() / 1e3);
            message.storeUint(BigInteger.valueOf(timestamp + 60L), 32);
        }
        return message;
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public Address getAddress() {
        if (isNull(address)) {
            return (createStateInit()).address;
        }
        return address;
    }

    public void deploy(Tonlib tonlib, byte[] secretKey) {
        tonlib.sendRawMessage(createInitExternalMessage(secretKey).message.toBase64());
    }

    /**
     * Sends amount of nano toncoins to destination addresses using auto-fetched seqno without the body and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    /**
     * Sends amount of nano toncoins to destination addresses using auto-fetched seqno with the body and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param body               byte[]
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, byte[] body) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    /**
     * Sends amount of nano toncoins to destination address using auto-fetched seqno with the body and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param body               Cell
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, Cell body) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    /**
     * Sends amount of nano toncoins to destination address using auto-fetched seqno with the body and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param body               byte[]
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, byte[] body) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    /**
     * Sends amount of nano toncoins to destination address using auto-fetched seqno with the body and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param body               Cell
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, Cell body) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    /**
     * Sends amount of nano toncoins to destination address using specified seqno with the comment and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param seqno              long
     * @param comment            String
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, String comment) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, CellBuilder.beginCell().storeUint(0, 32).storeString(comment).endCell());
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    /**
     * Sends amount of nano toncoins to destination address using specified seqno without comment and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param seqno              long
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    /**
     * Sends amount of nano toncoins to destination addresses using auto-fetched seqno without the body and with the comment and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param comment            String
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, String comment) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, CellBuilder.beginCell().storeUint(0, 32).storeString(comment).endCell());
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    /**
     * Sends amount of nano toncoins to destination addresses using auto-fetched seqno with the body and default send-mode 3
     *
     * @param tonlib              Tonlib
     * @param secretKey           byte[]
     * @param destinationAddress1 Address
     * @param destinationAddress2 Address
     * @param amount              BigInteger
     * @param body                byte[]
     */
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

    /**
     * Sends amount of nano toncoins to destination addresses using auto-fetched seqno without the body and with comment and default send-mode 3
     *
     * @param tonlib              Tonlib
     * @param secretKey           byte[]
     * @param destinationAddress1 Address
     * @param destinationAddress2 Address
     * @param amount              BigInteger
     */
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

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress1, Address destinationAddress2, Address destinationAddress3, Address destinationAddress4, BigInteger amount, String comment) {
        return sendTonCoins(tonlib, secretKey, destinationAddress1, destinationAddress2, destinationAddress3, destinationAddress4, amount, CellBuilder.beginCell().storeUint(0, 32).storeString(comment).endCell());
    }

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress1, Address destinationAddress2, Address destinationAddress3, Address destinationAddress4, BigInteger amount) {
        return sendTonCoins(tonlib, secretKey, destinationAddress1, destinationAddress2, destinationAddress3, destinationAddress4, amount, CellBuilder.beginCell().endCell());
    }
}
