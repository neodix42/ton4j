package org.ton.java.smartcontract.wallet.v5;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.smartcontract.types.Destination;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV5Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.ActionSendMsg;
import org.ton.java.tlb.types.ConfigParams1;
import org.ton.java.tlb.types.CurrencyCollection;
import org.ton.java.tlb.types.ExternalMessageInfo;
import org.ton.java.tlb.types.InternalMessageInfoRelaxed;
import org.ton.java.tlb.types.Message;
import org.ton.java.tlb.types.MessageRelaxed;
import org.ton.java.tlb.types.MsgAddressIntStd;
import org.ton.java.tlb.types.OutAction;
import org.ton.java.tlb.types.OutList;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.isNull;

@Builder
@Getter
public class WalletV5 implements Contract {

    private static final int SIZE_BOOL = 1;
    private static final int SIZE_SEQNO = 32;
    private static final int SIZE_WALLET_ID = 32;
    private static final int SIZE_VALID_UNTIL = 32;

    private static final int ADD_EXTENSION = 2;
    private static final int REMOVE_EXTENSION = 3;
    private static final int SIG_AUTH = 4;

    private static final int PREFIX_SIGNED_EXTERNAL = 0x7369676E;
    private static final int PREFIX_SIGNED_INTERNAL = 0x73696E74;
    private static final int PREFIX_EXTENSION_ACTION = 0x6578746e;

    long seqno;
    long walletId;
    int subWalletNumber;
    TonHashMapE extensions;
    boolean isSigAuthAllowed;
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
                .storeBit(isSigAuthAllowed)
                .storeUint(seqno, 32)
                .storeUint(walletId, 32)
                .storeBytes(keyPair.getPublicKey())
                .storeDict(extensions.serialize(
                        k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
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

    public ExtMessageInfo send(WalletV5Config config) {
        return tonlib.sendRawMessage(prepareExternalMsg(config).toCell().toBase64());
    }

    /**
     * Deploy wallet without any extensions.
     * One can be installed later into the wallet. See addExtension().
     */

    public ExtMessageInfo deploy(WalletV5Config conf) {
        return tonlib.sendRawMessage(prepareExternalMsg(conf).toCell().toBase64());
    }

    public Message prepareDeployMsg(WalletV5Config conf) {
        Cell body = createExternalTransferBody(conf);

        return Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .init(getStateInit())
                .body(CellBuilder.beginCell()
                        .storeCell(body)
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                        .endCell())
                .build();
    }

    public Message prepareExternalMsg(WalletV5Config config) {
        Cell body = createExternalTransferBody(config);

        return Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .init(getStateInit())
                .body(CellBuilder.beginCell()
                        .storeCell(body)
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                        .endCell())
                .build();
    }

    private Cell createExternalTransferBody(WalletV5Config config) {
        return CellBuilder.beginCell()
                .storeUint(config.getOp() == 0 ? PREFIX_SIGNED_EXTERNAL : config.getOp(), 32)
                .storeUint(config.getWalletId(), SIZE_WALLET_ID)
                .storeUint((config.getValidUntil() == 0) ? Instant.now().getEpochSecond() + 60 : config.getValidUntil(), SIZE_VALID_UNTIL)
                .storeUint(config.getSeqno(), SIZE_SEQNO)
                .storeCell(config.getBody().toCell()) // innerRequest
                .storeBit(config.isSignatureAllowed()) // for now empty
                .endCell();
    }

    public ExtMessageInfo addExtension(WalletV5Config config) {
        config.setOp(PREFIX_EXTENSION_ACTION);
        Cell body = createExternalTransferBody(config);
        Message msg = MsgUtils.createExternalMessageWithSignedBody(keyPair, getAddress(), null, body);
        return tonlib.sendRawMessage(msg.toCell().toBase64());
    }

    public ExtMessageInfo addExtensionInternal(WalletV5Config config) {
        config.setOp(PREFIX_SIGNED_INTERNAL);
        Cell body = createActionTransferBody(config);
        Message msg = MsgUtils.createInternalMessage(getAddress(), config.getAmount(), getStateInit(), body, config.isBounce());
        return tonlib.sendRawMessage(msg.toCell().toBase64());
    }


    private Cell createActionTransferBody(WalletV5Config config) {
        return CellBuilder.beginCell()
                .storeUint(PREFIX_SIGNED_EXTERNAL, 32)
                .storeUint(config.getQueryId(), 64)
                .storeCell(config.getBody().toCell()) // innerRequest
                .storeBit(config.isSignatureAllowed())
                .endCell();
    }

    public OutList createSingleTransfer(String address, BigInteger amount, Boolean bounce, String comment, Cell body) {
        Destination recipient = Destination.builder()
                .address(address)
                .amount(amount)
                .bounce(bounce)
                .comment(comment)
                .body(body)
                .build();

        OutAction action = convertDestinationToOutAction(recipient);

        return OutList.builder().actions(Collections.singletonList(action)).build();
    }

    public OutList createBulkTransfer(List<Destination> recipients) {
        if (recipients.size() > 255) {
            throw new IllegalArgumentException("Maximum number of recipients should be less than 255");
        }

        List<OutAction> messages = new ArrayList<>();
        for (Destination recipient : recipients) {
            messages.add(convertDestinationToOutAction(recipient));
        }

        return OutList.builder()
                .actions(messages)
                .build();
    }

    private OutAction convertDestinationToOutAction(Destination destination) {
        Address dstAddress = Address.of(destination.getAddress());
        return ActionSendMsg.builder()
                .mode((destination.getMode() == 0) ? 3 : destination.getMode())
                .outMsg(MessageRelaxed.builder()
                        .info(InternalMessageInfoRelaxed.builder()
                                .bounce(destination.isBounce())
                                .dstAddr(MsgAddressIntStd.builder()
                                        .workchainId(dstAddress.wc)
                                        .address(dstAddress.toBigInteger())
                                        .build())
                                .value(CurrencyCollection.builder()
                                        .coins(destination.getAmount())
                                        .build())
                                .build())
                        .init(getStateInit())
                        .body((isNull(destination.getBody()) && StringUtils.isNotEmpty(destination.getComment())) ?
                                CellBuilder.beginCell()
                                        .storeUint(0, 32) // 0 opcode means we have a comment
                                        .storeString(destination.getComment())
                                        .endCell() :
                                destination.getBody())
                        .build())
                .build();
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

    public List<ConfigParams1> getRawExtensions() {
        List<String> r = new ArrayList<>();
        RunResult result = tonlib.runMethod(getAddress(), "get_extensions");
        TvmStackEntryCell tvmStackEntryCell = (TvmStackEntryCell) result.getStack().get(0);
        String base64Msg = tvmStackEntryCell.getCell().getBytes();
        Cell cell = Cell.fromBocBase64(base64Msg);
        CellSlice cs = CellSlice.beginParse(cell);
        List<ConfigParams1> l = new ArrayList<>();
        // todo... how to convert
        while (!cs.isSliceEmpty()) {
            l.add(ConfigParams1.deserialize(cs));
        }
        System.out.println(l);
//        try {
//            TvmStackEntryList list = (TvmStackEntryList) result.getStack().get(0);
//            for (Object o : list.getList().getElements()) {
//                TvmStackEntryTuple t = (TvmStackEntryTuple) o;
//                TvmTuple tuple = t.getTuple();
//                TvmStackEntryNumber wc = (TvmStackEntryNumber) tuple.getElements().get(0); // 1 byte
//                TvmStackEntryNumber addr = (TvmStackEntryNumber) tuple.getElements().get(1); // 32 bytes
//                r.add(wc.getNumber() + ":" + addr.getNumber().toString(16).toUpperCase());
//            }
//            TvmStackEntryCell cell = (TvmStackEntryCell) result.getStack().get(0);
//            for (Object o : cell) {
//                TvmStackEntryTuple t = (TvmStackEntryTuple) o;
//                TvmTuple tuple = t.getTuple();
//                r.add(tuple.toString());
//            }
//        }
//        catch (Exception e) {
//
//        }
        return l;
    }
}