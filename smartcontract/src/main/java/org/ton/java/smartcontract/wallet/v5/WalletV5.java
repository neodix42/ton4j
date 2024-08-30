package org.ton.java.smartcontract.wallet.v5;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV5Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Builder
@Getter
public class WalletV5 implements Contract {

    private static final int SIZE_BOOL = 1;
    private static final int SIZE_SEQNO = 32;
    private static final int SIZE_WALLET_ID = 32;
    private static final int SIZE_VALID_UNTIL = 32;

    long seqno;
    long walletId;
    int subWalletNumber;
    TonHashMapE extensions;
    boolean isSignatureAllowed;
    TweetNaclFast.Signature.KeyPair keyPair;

    private Tonlib tonlib;
    private long wc;

    public static class WalletV5Builder {
    }

    public static WalletV5Builder builder() {
        return new CustomWalletV5Builder();
    }

    private static class CustomWalletV5Builder extends WalletV5Builder {
        @Override
        public WalletV5 build() {
            if (isNull(super.keyPair)) {
                super.keyPair = Utils.generateSignatureKeyPair();
            }
            return super.build();
        }
    }

    @Override
    public Tonlib getTonlib() {
        return tonlib;
    }

    @Override
    public long getWorkchain() {
        return wc;
    }

    @Override
    public String getName() {
        return "V5";
    }

    @Override
    public Cell createDataCell() {
        if (extensions == null) {
            extensions = new TonHashMapE(0);
        }
        return CellBuilder.beginCell()
                .storeBit(isSignatureAllowed)
                .storeUint(seqno, SIZE_SEQNO)
                .storeUint(walletId, SIZE_WALLET_ID)
                .storeBytes(keyPair.getPublicKey())
                .storeDict(extensions.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 256).endCell().getBits(),
                        v -> CellBuilder.beginCell().storeBit((Boolean) v).endCell()))
                .endCell();
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell()
                .fromBoc(WalletCodes.V5.getValue())
                .endCell();
    }

    /**
     * Deploy wallet without any extensions.
     * One can be installed later into the wallet. See addExtension().
     */

    public ExtMessageInfo deploy() {
        return tonlib.sendRawMessage(prepareDeployMsg().toCell().toBase64());
    }

    public Message prepareDeployMsg() {
        Cell body = createDeployMessage();
        return Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .init(getStateInit())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();
    }

    public Cell createDeployMessage() {
        return CellBuilder.beginCell()
                .storeUint(walletId, 32)
                .storeInt(-1, 32)
                .storeUint(seqno, 32)
                .endCell();
    }

    public ExtMessageInfo send(WalletV5Config config) {
        return tonlib.sendRawMessage(prepareExternalMsg(config).toCell().toBase64());
    }

    public Message prepareExternalMsg(WalletV5Config config) {
        Cell body = createTransferBody(config);
        return MsgUtils.createExternalMessageWithSignedBody(keyPair, getAddress(), null, body);
    }

    public Cell createTransferBody(WalletV5Config config) {
        CellBuilder message = CellBuilder.beginCell();

        message.storeUint(config.getWalletId(), SIZE_WALLET_ID);
        message.storeUint((config.getValidUntil() == 0) ? Instant.now().getEpochSecond() + 60 : config.getValidUntil(), SIZE_VALID_UNTIL);
        message.storeUint(config.getSeqno(), SIZE_SEQNO);
        message.storeCell(storeWalletActions(config.getExtensions()));

        return message.endCell();
    }

    private Cell storeWalletActions(WalletActions walletActions) {
        CellBuilder cb = CellBuilder.beginCell();
        if (ObjectUtils.isNotEmpty(walletActions.outSendMessageAction)) {
            while (ObjectUtils.isNotEmpty(walletActions.outSendMessageAction)) {
                cb.storeRefMaybe(walletActions.outSendMessageAction.remove(0).toCell());
            }
        }
        else {
            cb.storeBit(false);
        }
        if (ObjectUtils.isNotEmpty(walletActions.extendedActions)) {
            while (ObjectUtils.isNotEmpty(walletActions.extendedActions)) {
                cb.storeBit(true);
                cb.storeCell(storeExtendedActions(walletActions.extendedActions));
            }
        }
        else {
            cb.storeBit(false);
        }

        return cb.endCell();
    }

    private Cell storeExtendedActions(List<WalletActions.ExtendedAction> extendedActions) {
        CellBuilder cb = CellBuilder.beginCell();
        while (ObjectUtils.isNotEmpty(extendedActions)) {
            cb.storeRef(extendedActions.remove(0).toCell());
        }

        return cb.endCell();
    }

// Get Methods
// --------------------------------------------------------------------------------------------------

    public long getWalletId() {
        RunResult result = tonlib.runMethod(getAddress(), "get_subwallet_id");
        TvmStackEntryNumber subWalletId = (TvmStackEntryNumber) result.getStack().get(0);
        return subWalletId.getNumber().longValue();
    }

    public byte[] getPublicKey() {
        RunResult result = tonlib.runMethod(getAddress(), "get_public_key");
        TvmStackEntryNumber pubKey = (TvmStackEntryNumber) result.getStack().get(0);
        return pubKey.getNumber().toByteArray();
    }

    public boolean getIsSignatureAuthAllowed() {
        RunResult result = tonlib.runMethod(getAddress(), "is_signature_allowed");
        TvmStackEntryNumber signatureAllowed = (TvmStackEntryNumber) result.getStack().get(0);
        return signatureAllowed.getNumber().longValue() != 0;
    }

    public List<String> getRawExtensions() {
        List<String> r = new ArrayList<>();
        Address myAddress = getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_extensions");
        TvmStackEntryList list = (TvmStackEntryList) result.getStack().get(0);
        for (Object o : list.getList().getElements()) {
            TvmStackEntryTuple t = (TvmStackEntryTuple) o;
            TvmTuple tuple = t.getTuple();
            r.add(tuple.toString());
        }

        return r;
    }

    public List<WalletActions> getExtensionsList() {
        List<WalletActions> r = new ArrayList<>();
        Address myAddress = getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_extensions");
        TvmStackEntryList list = (TvmStackEntryList) result.getStack().get(0);
        for (Object o : list.getList().getElements()) {
            TvmStackEntryTuple t = (TvmStackEntryTuple) o;
            TvmTuple tuple = t.getTuple();
            // todo.. getExtension type
            TvmStackEntryNumber addr = (TvmStackEntryNumber) tuple.getElements().get(1); // 32 bytes
            r.add(WalletActions.builder()
//                    .walletAddress(Address.of(addr.getNumber().toString(16)))
//                    .type("")
                    .build());
        }
        return r;
    }
}