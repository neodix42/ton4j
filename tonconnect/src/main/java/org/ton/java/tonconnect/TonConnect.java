package org.ton.java.tonconnect;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.mnemonic.Ed25519;
import org.ton.java.tlb.types.StateInit;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.util.Objects.isNull;

public class TonConnect {

    //Unique prefix to separate messages from on-chain messages.
    public static final String TON_CONNECT = "ton-connect";

    public static boolean checkProof(TonProof tonProof, WalletAccount account) throws Exception {
        byte[] publicKeyBytes;
        if (StringUtils.isEmpty(account.getPublicKey()) || isNull(account.getPublicKey())) {
            StateInit stateInit = StateInit.deserialize(CellSlice.beginParse(CellBuilder.beginCell().fromBocBase64(account.getWalletStateInit()).endCell()));
            publicKeyBytes = CellSlice.beginParse(stateInit.getData()).skipBits(32).skipBits(32).loadBytes(256);
        } else {
            publicKeyBytes = Hex.decodeHex(account.getPublicKey());
        }
        byte[] signature = Base64.decode(tonProof.getSignature());
        byte[] messageForSigning = createMessageForSigning(tonProof, account.getAddress());

        return Ed25519.verify(publicKeyBytes, messageForSigning, signature);
    }

    /**
     * message = utf8_encode("ton-proof-item-v2/") ++
     * Address ++
     * AppDomain ++
     * Timestamp ++
     * Payload
     */
    private static byte[] createMessage(TonProof tonProof, String address) throws DecoderException {
        String addressWithoutWorkchain = address.replace("0:", "");
        byte[] addressBytes = Hex.decodeHex(addressWithoutWorkchain);
        long timestamp = tonProof.getTimestamp();
        int domainLength = tonProof.getDomain().getLengthBytes();
        String domainValue = tonProof.getDomain().getValue();
        String payload = tonProof.getPayload();

        // Create message
        byte[] proofItemPrefix = "ton-proof-item-v2/".getBytes(StandardCharsets.UTF_8);
        ByteBuffer messageBuffer = ByteBuffer.allocate(proofItemPrefix.length + 4 + addressBytes.length + 4 + domainValue.length() + 8 + payload.length());
        messageBuffer.put(proofItemPrefix);
        messageBuffer.putInt(Integer.reverseBytes(0)); // workchain, little-endian
        messageBuffer.put(addressBytes);
        messageBuffer.putInt(Integer.reverseBytes(domainLength)); // domain length, little-endian
        messageBuffer.put(domainValue.getBytes(StandardCharsets.UTF_8));
        messageBuffer.putLong(Long.reverseBytes(timestamp)); // timestamp, little-endian
        messageBuffer.put(payload.getBytes(StandardCharsets.UTF_8));

        return messageBuffer.array();
    }

    /**
     * Create message for signing
     * format: sha256( 0xffff ++ utf8_encode("ton-connect") ++ sha256(message) )
     * result sha256 of message for signing
     */
    public static byte[] createMessageForSigning(TonProof tonProof, String address) throws NoSuchAlgorithmException, DecoderException {
        byte[] message = createMessage(tonProof, address);
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] hashedMessage = sha256.digest(message);
        byte[] signatureMessage = new byte[2 + TON_CONNECT.length() + hashedMessage.length];
        signatureMessage[0] = (byte) 0xFF;
        signatureMessage[1] = (byte) 0xFF;
        System.arraycopy(TON_CONNECT.getBytes(StandardCharsets.UTF_8), 0, signatureMessage, 2, TON_CONNECT.length());
        System.arraycopy(hashedMessage, 0, signatureMessage, 2 + TON_CONNECT.length(), hashedMessage.length);

        return sha256.digest(signatureMessage);
    }
}
