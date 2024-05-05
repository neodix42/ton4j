package org.ton.java.smartcontract.wallet.v3;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletV3Config;
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

public class WalletV3ContractBase implements Contract<WalletV3Config> {
    Options options;
    Address address;

    protected WalletV3ContractBase(Options options) {
        this.options = options;
    }

    @Override
    public String getName() {
        return "override me";
    }

    /**
     * Creates message payload with subwallet-id, valid-until and seqno, equivalent to:
     * <b subwallet-id 32 u, timestamp 32 i, seqno 32 u, b> // signing message
     */
    @Override
    public Cell createTransferBody(WalletV3Config config) {

        CellBuilder message = CellBuilder.beginCell();

        message.storeUint(BigInteger.valueOf(getOptions().walletId), 32);

        if (config.getSeqno() == 0) {
            for (int i = 0; i < 32; i++) {
                message.storeBit(true);
            }
        } else {
            Date date = new Date();
            long timestamp = (long) Math.floor(date.getTime() / 1e3);
            message.storeUint(BigInteger.valueOf(timestamp + 60L), 32);
        }

        message.storeUint(BigInteger.valueOf(config.getSeqno()), 32);
        return message.endCell();
    }

    @Override
    public Options getOptions() {
        return options;
    }


    /**
     * <b 0 32 u, subwallet-id 32 u, file-base +".pk" load-generate-keypair B, b> // data cell
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(BigInteger.ZERO, 32);
        cell.storeUint(BigInteger.valueOf(getOptions().walletId), 32);
        cell.storeBytes(getOptions().publicKey);
        return cell.endCell();
    }

    public long getWalletId() {
        return getOptions().walletId;
    }

    @Override
    public ExtMessageInfo deploy(Tonlib tonlib, WalletV3Config config) {
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

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, WalletV3Config config) {
        long seqno = getSeqno(tonlib);
        config.setSeqno(seqno);
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
    /*

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, byte[] body) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, Cell body) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, byte[] body, byte sendMode) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body, sendMode);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, byte[] body) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, Cell body) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, String comment) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, CellBuilder.beginCell().storeUint(0, 32).storeString(comment).endCell());
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, String comment) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, CellBuilder.beginCell().storeUint(0, 32).storeString(comment).endCell());
        return tonlib.sendRawMessage(msg.message.toBase64());
    }

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, byte[] body, byte sendMode) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body, sendMode);
        return tonlib.sendRawMessage(msg.message.toBase64());
    }
    */
}
