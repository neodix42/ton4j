package org.ton.java.smartcontract.wallet;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.InitExternalMessage;
import org.ton.java.smartcontract.types.StateInit;
import org.ton.java.smartcontract.wallet.v1.WalletV1ContractR1;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;

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
        return cell.endCell();
    }

    /**
     * @param seqno long
     * @return Cell
     */
    default CellBuilder createSigningMessage(long seqno) {
        return CellBuilder.beginCell().storeUint(BigInteger.valueOf(seqno), 32);
    }

    /**
     * External message for initialization.
     * <p>
     * If you use KeyPair based on mnemonic:
     * <p>
     * <code>Pair keyPair = Mnemonic.toKeyPair(Mnemonic.generate(24));</code>
     * <p>
     * Then you must build signature KeyPair out of it:
     * <p>
     * <code>TweetNaclFast.Signature.KeyPair keyPairSig = TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());</code>
     * <p>
     * and use it in createInitExternalMessage as a secretKey:
     * <p>
     * <code>contract.createInitExternalMessage(keyPairSig.getSecretKey());</code>
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

        Cell signingMessage = createSigningMessage(0).endCell();
        byte[] signature = new TweetNaclFast.Signature(getOptions().publicKey, secretKey).detached(signingMessage.hash());

        Cell body = CellBuilder.beginCell()
                .storeBytes(signature)
                .storeCell(signingMessage)
                .endCell();

        Cell header = Contract.createExternalMessageHeader(stateInit.address);

        Cell externalMessage = Contract.createCommonMsgInfo(header, stateInit.stateInit, body);

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
     * External message for initialization.
     * <p>
     * If you use KeyPair based on mnemonic:
     * <p>
     * <code>Pair keyPair = Mnemonic.toKeyPair(Mnemonic.generate(24));</code>
     * <p>
     * Then you must build signature KeyPair out of it:
     * <p>
     * <code>TweetNaclFast.Signature.KeyPair keyPairSig = TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());</code>
     * <p>
     * and use it in createInitExternalMessage as a secretKey:
     * <p>
     * <code>contract.createInitExternalMessage(keyPairSig.getSecretKey());</code>
     *
     * @param secretKey byte[] nacl.KeyPair.secretKey
     * @return InitExternalMessage
     */
    default InitExternalMessage createInitExternalMessageWithoutBody(byte[] secretKey) {

        if (getOptions().publicKey.length == 0) {
            TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);
            getOptions().publicKey = keyPair.getPublicKey();
        }

        StateInit stateInit = createStateInit();

        Cell signingMessage = CellBuilder.beginCell().endCell();
        Cell body = CellBuilder.beginCell().endCell();

        Cell header = Contract.createExternalMessageHeader(stateInit.address);

        Cell externalMessage = Contract.createCommonMsgInfo(header, stateInit.stateInit, body);

        return new InitExternalMessage(
                stateInit.address,
                externalMessage,
                body,
                signingMessage,
                stateInit.stateInit,
                stateInit.code,
                stateInit.data);
    }

    default Cell createSignedMessage(byte[] signature, Cell signingMessage) {
        CellBuilder msg = CellBuilder.beginCell();

        msg.storeBytes(signature);
        msg.storeCell(signingMessage);

        return msg.endCell();
    }

    /**
     * Creates external message signed by the specified secretKey.
     * <p>
     * If you use KeyPair based on mnemonic, you have to convert it to Signature KeyPair:
     * <p>
     * <code>TweetNaclFast.Signature.KeyPair keyPairSig = TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());</code>
     *
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
        Cell body = createSignedMessage(signature, signingMessage);

        Cell stateInit = null;
        Cell code = null;
        Cell data = null;

        if (seqno == 0) {
            if (isNull(getOptions().publicKey)) {
                TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(secretKey);
                getOptions().publicKey = keyPair.getPublicKey();
            }
            StateInit deploy = createStateInit();
            stateInit = deploy.stateInit;
            code = deploy.code;
            data = deploy.data;
        }

        Address selfAddress = getAddress();
        Cell header = Contract.createExternalMessageHeader(selfAddress);
        Cell resultMessage = Contract.createCommonMsgInfo(header, stateInit, body);

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

    default ExternalMessage createExternalMessage(Cell signingMessage,
                                                  byte[] secretKey,
                                                  long seqno) {
        return createExternalMessage(signingMessage, secretKey, seqno, false);
    }

    /**
     * Creates transfer message with a various parameters signed by the specified secretKey.
     * <p>
     * If you use KeyPair based on mnemonic, you have to convert it to Signature KeyPair:
     * <p>
     * <code>TweetNaclFast.Signature.KeyPair keyPairSig = TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());</code>
     *
     * @param secretKey      byte[]  nacl.KeyPair.secretKey
     * @param address        Address destination
     * @param amount         BigInteger in nano-coins
     * @param seqno          long
     * @param payload        Cell, null
     * @param sendMode       byte, 3
     * @param stateInit      Cell, null
     * @param dummySignature boolean, false
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            Address address,
            BigInteger amount,
            long seqno,
            Cell payload,
            byte sendMode,
            Cell stateInit,
            boolean dummySignature) {

        Cell orderHeader = Contract.createInternalMessageHeader(address, amount);
        Cell order = Contract.createCommonMsgInfo(orderHeader, stateInit, payload);
        Cell signingMessage = createSigningMessage(seqno)
                .storeUint(sendMode & 0xff, 8)
                .storeRef(order)
                .endCell();

        return createExternalMessage(signingMessage, secretKey, seqno, dummySignature);
    }

    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            Address address,
            BigInteger amount,
            long seqno,
            Cell payload,
            byte sendMode,
            Cell stateInit) {
        return createTransferMessage(secretKey, address, amount, seqno, payload, sendMode, stateInit, false);
    }

    /**
     * Create transfer message with send-mode 3
     *
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
            long seqno,
            Cell payload) {

        Cell stateInit = null;
        return createTransferMessage(secretKey, address, amount, seqno, payload, (byte) 3, stateInit, false);
    }

    /**
     * Create transfer message with send-mode 3
     *
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
            long seqno,
            byte[] payload) {

        Cell stateInit = null;
        return createTransferMessage(secretKey, address, amount, seqno, payload, (byte) 3, stateInit, false);
    }

    /**
     * @param secretKey byte[]  nacl.KeyPair.secretKey
     * @param address   Address
     * @param amount    BigInteger in nano-coins
     * @param seqno     long
     * @param payload   Cell
     * @param sendMode  byte
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            Address address,
            BigInteger amount,
            long seqno,
            Cell payload,
            byte sendMode) {

        Cell stateInit = null;
        return createTransferMessage(secretKey, address, amount, seqno, payload, sendMode, stateInit, false);
    }

    /**
     * @param secretKey byte[]  nacl.KeyPair.secretKey
     * @param address   String
     * @param amount    BigInteger in nano-coins
     * @param seqno     long
     * @param payload   Cell
     * @param sendMode  byte
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            String address,
            BigInteger amount,
            long seqno,
            Cell payload,
            byte sendMode) {

        Cell stateInit = null;
        return createTransferMessage(secretKey, address, amount, seqno, payload, sendMode, stateInit, false);
    }

    /**
     * @param secretKey byte[]  nacl.KeyPair.secretKey
     * @param address   String
     * @param amount    BigInteger in nano-coins
     * @param seqno     long
     * @param payload   Cell
     * @param sendMode  byte
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            String address,
            BigInteger amount,
            long seqno,
            byte[] payload,
            byte sendMode) {

        Cell stateInit = null;
        return createTransferMessage(secretKey, address, amount, seqno, payload, sendMode, stateInit, false);
    }

    /**
     * @param secretKey byte[]  nacl.KeyPair.secretKey
     * @param address   Address
     * @param amount    BigInteger in nano-coins
     * @param seqno     long
     * @param payload   Cell
     * @param sendMode  byte
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            Address address,
            BigInteger amount,
            long seqno,
            byte[] payload,
            byte sendMode) {

        Cell stateInit = null;
        return createTransferMessage(secretKey, address, amount, seqno, payload, sendMode, stateInit, false);
    }

    /**
     * Create transfer message with send-mode 3
     *
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
        return createTransferMessage(secretKey, address, amount, seqno, payload, (byte) 3, stateInit, false);
    }

    /**
     * Create transfer message with send-mode 3
     *
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
        return createTransferMessage(secretKey, Address.of(address), amount, seqno, payload, (byte) 3, stateInit, false);
    }

    /**
     * @param secretKey      byte[] nacl.KeyPair.secretKey
     * @param address        String
     * @param amount         BigInteger in nano-coins
     * @param seqno          long
     * @param payload        Cell
     * @param sendMode       byte, 3
     * @param stateInit      Cell, null
     * @param dummySignature boolean, false
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            String address,
            BigInteger amount,
            long seqno,
            Cell payload,
            byte sendMode,
            Cell stateInit,
            boolean dummySignature) {
        return createTransferMessage(secretKey, Address.of(address), amount, seqno, payload, sendMode, stateInit, dummySignature);
    }

    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            String address,
            BigInteger amount,
            long seqno,
            Cell payload,
            byte sendMode,
            Cell stateInit) {
        return createTransferMessage(secretKey, Address.of(address), amount, seqno, payload, sendMode, stateInit, false);
    }

    /**
     * @param secretKey      byte[]  nacl.KeyPair.secretKey
     * @param address        Address
     * @param amount         BigInteger in nano-coins
     * @param seqno          long
     * @param payload        byte[]
     * @param sendMode       byte, 3
     * @param stateInit      Cell, null
     * @param dummySignature boolean, false
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            Address address,
            BigInteger amount,
            long seqno,
            byte[] payload,
            byte sendMode,
            Cell stateInit,
            boolean dummySignature) {

        Cell payloadCell = CellBuilder.beginCell().storeBytes(payload).endCell();

        return createTransferMessage(secretKey, address, amount, seqno, payloadCell, sendMode, stateInit, dummySignature);
    }

    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            Address address,
            BigInteger amount,
            long seqno,
            byte[] payload,
            byte sendMode,
            Cell stateInit) {

        Cell payloadCell = CellBuilder.beginCell().storeBytes(payload).endCell();

        return createTransferMessage(secretKey, address, amount, seqno, payloadCell, sendMode, stateInit, false);
    }

    /**
     * @param secretKey      byte[]  nacl.KeyPair.secretKey
     * @param address        String
     * @param amount         BigInteger in nano-coins
     * @param seqno          long
     * @param payload        byte[]
     * @param sendMode       byte, 3
     * @param stateInit      Cell, null
     * @param dummySignature boolean, false
     * @return ExternalMessage
     */
    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            String address,
            BigInteger amount,
            long seqno,
            byte[] payload,
            byte sendMode,
            Cell stateInit,
            boolean dummySignature) {
        return createTransferMessage(secretKey, Address.of(address), amount, seqno, payload, sendMode, stateInit, dummySignature);
    }

    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            String address,
            BigInteger amount,
            long seqno,
            byte[] payload,
            byte sendMode,
            Cell stateInit) {
        return createTransferMessage(secretKey, Address.of(address), amount, seqno, payload, sendMode, stateInit, false);
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
            Cell stateInit,
            boolean dummySignature) {

        CellBuilder payloadCell = CellBuilder.beginCell();

        if (payload.length() > 0) {
            payloadCell.storeUint(BigInteger.ZERO, 32);
            payloadCell.storeString(payload);
            System.err.println("payload is empty");
        } else {
            payloadCell = null;
        }

        return createTransferMessage(secretKey, address, amount, seqno, payloadCell.endCell(), sendMode, stateInit, dummySignature);
    }

    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            Address address,
            BigInteger amount,
            long seqno,
            String payload,
            byte sendMode,
            Cell stateInit) {
        return createTransferMessage(secretKey, address, amount, seqno, payload, sendMode, stateInit, false);
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
            Cell stateInit,
            boolean dummySignature) {
        return createTransferMessage(secretKey, Address.of(address), amount, seqno, payload, sendMode, stateInit, dummySignature);
    }

    default ExternalMessage createTransferMessage(
            byte[] secretKey,
            String address,
            BigInteger amount,
            long seqno,
            String payload,
            byte sendMode,
            Cell stateInit) {
        return createTransferMessage(secretKey, Address.of(address), amount, seqno, payload, sendMode, stateInit, false);
    }

    /**
     * Get current seqno
     *
     * @return long
     */
    default long getSeqno(Tonlib tonlib) {

        if (this instanceof WalletV1ContractR1) {
            throw new Error("Wallet V1R1 does not have seqno method");
        }

        Address myAddress = getAddress();
        return tonlib.getSeqno(myAddress);
    }
}
