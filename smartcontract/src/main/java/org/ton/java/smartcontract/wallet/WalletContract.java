package org.ton.java.smartcontract.wallet;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.InitExternalMessage;
import org.ton.java.smartcontract.types.StateInit;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

/**
 * Abstract standard wallet class
 */
public interface WalletContract extends Contract {

    String getName();

    /**
     * Method to override
     *
     * @return Cell cell, contains wallet data
     */
    @Override
    default Cell createDataCell() {
        // 4 byte seqno + 32 byte publicKey
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(BigInteger.ZERO, 32); // seqno
        cell.storeBytes(getOptions().publicKey);
        return cell;
    }

    /**
     * @param seqno long
     * @return Cell
     */
    default Cell createSigningMessage(long seqno) {
        return CellBuilder.beginCell().storeUint(BigInteger.valueOf(seqno), 32).endCell();
    }

    /**
     * External message for initialization
     *
     * @param secretKey byte[] nacl.KeyPair.secretKey
     * @return InitExternalMessage
     */
    default InitExternalMessage createInitExternalMessage(byte[] secretKey) {

        if (getOptions().publicKey.length == 0) {
            TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);
            getOptions().publicKey = keyPair.getPublicKey();
        }

        StateInit stateInit = createStateInit();

        Cell signingMessage = createSigningMessage(0);
        byte[] signature = new TweetNaclFast.Signature(getOptions().publicKey, secretKey).detached(signingMessage.hash());

        CellBuilder body = CellBuilder.beginCell();
        body.storeBytes(signature);
        body.writeCell(signingMessage);

        Cell header = Contract.createExternalMessageHeader(stateInit.address);

        Cell externalMessage = Contract.createCommonMsgInfo(header, stateInit.stateInit, body.endCell());

        return new InitExternalMessage(
                stateInit.address,
                externalMessage,
                body,
                signingMessage,
                stateInit.stateInit,
                stateInit.code,
                stateInit.data);
    }

    /**
     * @param signingMessage Cell
     * @param secretKey      byte[]  nacl.KeyPair.secretKey
     * @param seqno          long
     * @param dummySignature boolean, flag to specify whether to use signature based on private key or fill the space with zeros.
     * @return ExternalMessage
     */
    default ExternalMessage createExternalMessage(Cell signingMessage,
                                                  byte[] secretKey,
                                                  long seqno,
                                                  boolean dummySignature) {
        byte[] signature;
        if (dummySignature) {
            signature = new byte[64];
        } else {
            signature = Utils.signData(getOptions().publicKey, secretKey, signingMessage.hash());
        }

        CellBuilder body = CellBuilder.beginCell();
        body.storeBytes(signature);
        body.writeCell(signingMessage);

        Cell stateInit = null;
        Cell code = null;
        Cell data = null;

        if (seqno == 0) {
            if (getOptions().publicKey == null) {
                TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(secretKey); //) TweetNaclFast.Box.keyPair_fromSecretKey(
                getOptions().publicKey = keyPair.getPublicKey();
            }
            StateInit deploy = createStateInit();
            stateInit = deploy.stateInit;
            code = deploy.code;
            data = deploy.data;
        }

        Address selfAddress = getAddress();
        Cell header = Contract.createExternalMessageHeader(selfAddress);
        Cell resultMessage = Contract.createCommonMsgInfo(header, stateInit, body.endCell());

        return new ExternalMessage(
                getAddress(),
                resultMessage,
                body,
                signature,
                signingMessage,
                stateInit,
                code,
                data);
    }

    /**
     * @param secretKey      byte[]  nacl.KeyPair.secretKey
     * @param address        Address destination
     * @param amount         BigInteger in nano-coins
     * @param seqno          long
     * @param payload        Cell, null
     * @param sendMode       byte, 3
     * @param dummySignature boolean, false
     * @param stateInit      Cell, null
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            Address address,
            BigInteger amount,
            long seqno,
            Cell payload,
            byte sendMode,
            boolean dummySignature,
            Cell stateInit) {

        Cell orderHeader = Contract.createInternalMessageHeader(address, amount);
        Cell order = Contract.createCommonMsgInfo(orderHeader, stateInit, payload);
        Cell signingMessage = createSigningMessage(seqno);
        signingMessage.bits.writeUint8(sendMode & 0xff);
        signingMessage.refs.add(order);

        return createExternalMessage(signingMessage, secretKey, seqno, dummySignature);
    }

    /**
     * @param secretKey byte[]  nacl.KeyPair.secretKey
     * @param address   Address
     * @param amount    BigInteger in nano-coins
     * @param seqno     long
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            Address address,
            BigInteger amount,
            long seqno) {

        Cell payload = null;
        Cell stateInit = null;
        return createTransferMessage(secretKey, address, amount, seqno, payload, (byte) 3, false, stateInit);
    }

    /**
     * @param secretKey byte[]  nacl.KeyPair.secretKey
     * @param address   String
     * @param amount    BigInteger in nano-coins
     * @param seqno     long
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            String address,
            BigInteger amount,
            long seqno) {

        Cell payload = null;
        Cell stateInit = null;
        return createTransferMessage(secretKey, Address.of(address), amount, seqno, payload, (byte) 3, false, stateInit);
    }

    /**
     * @param secretKey      byte[] nacl.KeyPair.secretKey
     * @param address        String
     * @param amount         BigInteger in nano-coins
     * @param seqno          long
     * @param payload        Cell
     * @param sendMode       byte, 3
     * @param dummySignature boolean, false
     * @param stateInit      Cell, null
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            String address,
            BigInteger amount,
            long seqno,
            Cell payload,
            byte sendMode,
            boolean dummySignature,
            Cell stateInit) {
        return createTransferMessage(secretKey, Address.of(address), amount, seqno, payload, sendMode, dummySignature, stateInit);
    }

    /**
     * @param secretKey      byte[]  nacl.KeyPair.secretKey
     * @param address        Address
     * @param amount         BigInteger in nano-coins
     * @param seqno          long
     * @param payload        byte[]
     * @param sendMode       byte, 3
     * @param dummySignature boolean, false
     * @param stateInit      Cell, null
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            Address address,
            BigInteger amount,
            long seqno,
            byte[] payload,
            byte sendMode,
            boolean dummySignature,
            Cell stateInit) {

        Cell payloadCell = CellBuilder.beginCell().storeBytes(payload).endCell();

        return createTransferMessage(secretKey, address, amount, seqno, payloadCell, sendMode, dummySignature, stateInit);
    }

    /**
     * @param secretKey      byte[]  nacl.KeyPair.secretKey
     * @param address        String
     * @param amount         BigInteger in nano-coins
     * @param seqno          long
     * @param payload        byte[]
     * @param sendMode       byte, 3
     * @param dummySignature boolean, false
     * @param stateInit      Cell, null
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            String address,
            BigInteger amount,
            long seqno,
            byte[] payload,
            byte sendMode,
            boolean dummySignature,
            Cell stateInit) {
        return createTransferMessage(secretKey, Address.of(address), amount, seqno, payload, sendMode, dummySignature, stateInit);
    }

    /**
     * @param secretKey      byte[]  nacl.KeyPair.secretKey
     * @param address        Address
     * @param amount         BigInteger in nano-coins
     * @param seqno          long
     * @param payload        String
     * @param sendMode       byte, default 3
     * @param dummySignature boolean, false
     * @param stateInit      Cell, null
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            Address address,
            BigInteger amount,
            long seqno,
            String payload,
            byte sendMode,
            boolean dummySignature,
            Cell stateInit) {

        CellBuilder payloadCell = CellBuilder.beginCell();

        if (payload.length() > 0) {
            payloadCell.storeUint(BigInteger.ZERO, 32);
            payloadCell.storeString(payload);
            System.err.println("payload is empty");
        } else {
            payloadCell = null;
        }

        return createTransferMessage(secretKey, address, amount, seqno, payloadCell, sendMode, dummySignature, stateInit);
    }

    /**
     * @param secretKey      byte[]  nacl.KeyPair.secretKey
     * @param address        String
     * @param amount         BigInteger in nano-coins
     * @param seqno          long
     * @param payload        String
     * @param sendMode       byte
     * @param dummySignature boolean
     * @param stateInit      Cell
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            String address,
            BigInteger amount,
            long seqno,
            String payload,
            byte sendMode,
            boolean dummySignature,
            Cell stateInit) {
        return createTransferMessage(secretKey, Address.of(address), amount, seqno, payload, sendMode, dummySignature, stateInit);
    }
}
