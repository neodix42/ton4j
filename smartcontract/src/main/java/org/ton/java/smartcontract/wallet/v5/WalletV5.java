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
import org.ton.java.tlb.types.ActionSendMsg;
import org.ton.java.tlb.types.ExternalMessageInfo;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

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
        return CellBuilder.beginCell()
                .storeBit(isSignatureAllowed)
                .storeUint(seqno, 32)
                .storeUint(walletId, 32)
                .storeBytes(keyPair.getPublicKey())
                .storeDict(extensions.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 256).endCell().getBits(),
                        v -> CellBuilder.beginCell().storeBit((Boolean) v).endCell()
                ))
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

    public ExtMessageInfo deploy(WalletV5Config conf) {
        return tonlib.sendRawMessage(prepareDeployMsg(conf).toCell().toBase64());
    }

    public Message prepareDeployMsg(WalletV5Config conf) {
        Cell body = createTransferBody(conf);

        return Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .init(getStateInit())
//                .body(CellBuilder.beginCell()
//                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
//                        .storeCell(body)
//                        .endCell())
                .body(body)
                .build();
    }

    public ExtMessageInfo send(WalletV5Config config) {
        return tonlib.sendRawMessage(prepareExternalMsg(config).toCell().toBase64());
    }

    public Message prepareExternalMsg(WalletV5Config config) {
        Cell body = createTransferBody(config);
        return MsgUtils.createExternalMessageWithSignedBody(keyPair, getAddress(), null, body);
    }

    public Cell createTransferBody(WalletV5Config config) {
        Cell body = CellBuilder.beginCell()
                .storeUint(0x7369676e, 32)
                .storeUint(config.getWalletId(), SIZE_WALLET_ID)
                .storeUint((config.getValidUntil() == 0) ? Instant.now().getEpochSecond() + 60 : config.getValidUntil(), SIZE_VALID_UNTIL)
                .storeUint(config.getSeqno(), SIZE_SEQNO)
//                .storeCell(storeWalletActions(config.getExtensions())) // innerRequest
                .storeBit(false) // for now empty
                .endCell();

        return CellBuilder.beginCell()
                .storeCell(body)
                .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash())).endCell();
    }

    private Cell storeWalletActions(WalletActions walletActions) {
        CellBuilder cb = CellBuilder.beginCell();

        if (ObjectUtils.isNotEmpty(walletActions.outSendMessageAction)) {
            Cell outSendMessageTree = createActionTree(walletActions.outSendMessageAction);
            cb.storeRef(outSendMessageTree);
        } else {
            cb.storeBit(false);
        }

        if (ObjectUtils.isNotEmpty(walletActions.extendedActions)) {
            cb.storeBit(true);
            Cell extendedActionTree = createActionTree(walletActions.extendedActions);
            cb.storeRef(extendedActionTree);
        } else {
            cb.storeBit(false);
        }

        return cb.endCell();
    }

    private Cell createActionTree(List<?> actions) {
        if (actions.size() <= 4) {
            return createLeafCell(actions, 0, actions.size());
        }
        return createTreeRecursive(actions, 0, actions.size());
    }

    private Cell createTreeRecursive(List<?> actions, int start, int end) {
        if (end - start <= 4) {
            return createLeafCell(actions, start, end);
        }

        CellBuilder cb = CellBuilder.beginCell();
        int itemsPerSubtree = (end - start + 3) / 4;

        for (int i = 0; i < 4; i++) {
            int subStart = start + i * itemsPerSubtree;
            int subEnd = Math.min(subStart + itemsPerSubtree, end);
            if (subStart < end) {
                Cell subTree = createTreeRecursive(actions, subStart, subEnd);
                cb.storeRef(subTree);
            }
        }

        return cb.endCell();
    }

    private Cell createLeafCell(List<?> actions, int start, int end) {
        CellBuilder cb = CellBuilder.beginCell();
        for (int i = start; i < end; i++) {
            Object action = actions.get(i);
            if (action instanceof ActionSendMsg) {
                cb.storeRef(((ActionSendMsg) action).toCell());
            } else if (action instanceof WalletActions.ExtendedAction) {
                cb.storeRef(((WalletActions.ExtendedAction) action).toCell());
            }
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

    // todo.. introduce List<? extends deserialization type>
//    public List<WalletActions> getExtensionsList() {
//        List<WalletActions> r = new ArrayList<>();
//        Address myAddress = getAddress();
//        RunResult result = tonlib.runMethod(myAddress, "get_extensions");
//        TvmStackEntryList list = (TvmStackEntryList) result.getStack().get(0);
//        for (Object o : list.getList().getElements()) {
//            TvmStackEntryTuple t = (TvmStackEntryTuple) o;
//            TvmTuple tuple = t.getTuple();
//            TvmStackEntryNumber addr = (TvmStackEntryNumber) tuple.getElements().get(1); // 32 bytes
//            r.add(WalletActions.builder()
////                    .walletAddress(Address.of(addr.getNumber().toString(16)))
////                    .type("")
//                    .build());
//        }
//        return r;
//    }
}