package org.ton.java.smartcontract.multisig;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.util.*;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.address.Address;
import org.ton.java.cell.*;
import org.ton.java.smartcontract.types.*;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

@Builder
@Getter
@Log
public class MultiSigWallet implements Contract {

    //https://github.com/akifoq/multisig/blob/master/multisig-code.fc
    TweetNaclFast.Signature.KeyPair keyPair;
    long walletId;
    MultiSigConfig config;

    /**
     * interface to <a href="https://github.com/akifoq/multisig/blob/master/multisig-code.fc">multisig wallet smart-contract</a>
     * <p>
     * mandatory -  highloadQueryId, walletId, publicKey
     */

    public static class MultiSigWalletBuilder {
    }

    public static MultiSigWalletBuilder builder() {
        return new CustomMultiSigWalletBuilder();
    }

    private static class CustomMultiSigWalletBuilder extends MultiSigWalletBuilder {
        @Override
        public MultiSigWallet build() {
            if (isNull(super.keyPair)) {
                super.keyPair = Utils.generateSignatureKeyPair();
            }
            return super.build();
        }
    }

    private Tonlib tonlib;
    private long wc;

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
        return "multisig";
    }


    /**
     * Initial contract storage (init state).
     * Creator/deployer will be always part of k signatures.
     * By default, it will reside in owner_infos dict at index 0.
     *
     * @return Cell
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();

        cell.storeUint(walletId, 32);
        cell.storeUint(config.getN(), 8); // n
        cell.storeUint(config.getK(), 8); // k - collect at least k signatures
        cell.storeUint(0, 64); // last cleaned
        if (isNull(config.getOwners()) || config.getOwners().isEmpty()) {
            cell.storeBit(false); // initial owners dict
        } else {
            cell.storeDict(createOwnersInfoDict(config.getOwners()));
        }

        if (isNull(config.getPendingQueries()) || config.getPendingQueries().isEmpty()) {
            cell.storeBit(false); // initial  pending queries dict
        } else {
            cell.storeDict(createPendingQueries(config.getPendingQueries(), config.getN()));
        }

        return cell.endCell();
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.multisig.getValue()).
                endCell();
    }

    private Cell createSigningMessageInternal(int pubkeyIndex, Cell order) {
        return CellBuilder.beginCell()
                .storeUint(pubkeyIndex, 8) // root-id - pk-index for owner_infos dict
                .storeCell(order)
                .endCell();
    }


    public List<BigInteger> getPublicKeys() {

        List<BigInteger> publicKeys = new ArrayList<>();

        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_public_keys");

        if (result.getExit_code() != 0) {
            throw new Error("method get_public_keys, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryCell cellResult = (TvmStackEntryCell) result.getStack().get(0);
        Cell cell = CellBuilder.beginCell().fromBocBase64(cellResult.getCell().getBytes()).endCell();
//        Cell cell = Cell.fromBocBase64(cellResult.getCell().getBytes());

        CellSlice cs = CellSlice.beginParse(cell);
        TonHashMap loadedDict = cs.loadDict(8,
                k -> k.readUint(8), // index
                v -> v // ownerInfo cell
        );
        for (Map.Entry<Object, Object> entry : loadedDict.elements.entrySet()) {
            CellSlice cSlice = CellSlice.beginParse((Cell) entry.getValue());
            BigInteger pubKey = cSlice.loadUint(256);
            publicKeys.add(pubKey);
        }
        return publicKeys;
    }

    public List<String> getPublicKeysHex() {
        List<BigInteger> l = getPublicKeys();
        List<String> result = new ArrayList<>();
        for (BigInteger i : l) {
            result.add(i.toString(16));
        }
        return result;
    }

    /**
     * generates and returns init-state onchain
     *
     * @param walletId   walletid
     * @param n          total keys
     * @param k          minimum number of keys
     * @param ownersInfo arrays with public keys
     * @return cell with state-init
     */
    public Cell getInitState(long walletId, int n, int k, Cell ownersInfo) {

        Address myAddress = this.getAddress();
        Deque<String> stack = new ArrayDeque<>();

        stack.offer("[num, " + walletId + "]");
        stack.offer("[num, " + n + "]");
        stack.offer("[num, " + k + "]");
        stack.offer("[cell, " + ownersInfo.toHex(false) + "]");
        RunResult result = tonlib.runMethod(myAddress, "create_init_state", stack);

        if (result.getExit_code() != 0) {
            throw new Error("method createInitState, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryCell domainCell = (TvmStackEntryCell) result.getStack().get(0);
        return CellBuilder.beginCell().fromBocBase64(domainCell.getCell().getBytes()).endCell();
    }

    /**
     * Sends an external msg with the order containing all collected signatures signed by owner at index
     * pubkeyIndex with keyPair.
     *
     * @param keyPair TweetNaclFast.Signature.KeyPair
     */
    public ExtMessageInfo sendOrder(TweetNaclFast.Signature.KeyPair keyPair, int pubkeyIndex, Cell order) {

        Cell signingMessageBody = createSigningMessageInternal(pubkeyIndex, order);

        Message externalMessage = Message.builder()
                .info(ExternalMessageInInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), signingMessageBody.hash()))
                        .storeCell(signingMessageBody)
                        .endCell())
                .build();
        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }

    /**
     * Sends an external msg with the order containing all collected signatures signed by owner at index
     * pubkeyIndex with secretKey.
     *
     * @param secretKey byte[]
     */
    public ExtMessageInfo sendOrder(byte[] secretKey, int pubkeyIndex, Cell order) {
        Cell signingMessageBody = createSigningMessageInternal(pubkeyIndex, order);

        Message externalMessage = Message.builder()
                .info(ExternalMessageInInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), secretKey, signingMessageBody.hash()))
                        .storeCell(signingMessageBody)
                        .endCell())
                .build();
        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }

    /**
     * Serializes list of multisig wallet owners.
     *
     * @param ownersInfo OwnerInfo
     * @return Cell
     */
    public Cell createOwnersInfoDict(List<OwnerInfo> ownersInfo) {
        int dictKeySize = 8;
        TonHashMapE dictDestinations = new TonHashMapE(dictKeySize);

        long i = 0; // key, index 16bit
        for (OwnerInfo ownerInfo : ownersInfo) {

            CellBuilder ownerInfoCell = CellBuilder.beginCell();
            ownerInfoCell.storeBytes(ownerInfo.getPublicKey()); //256 bits
            ownerInfoCell.storeUint(ownerInfo.getFlood(), 8);

            dictDestinations.elements.put(
                    i++, // key - index
                    ownerInfoCell.endCell() // value - cell - OwnerInfo
            );
        }

        Cell cellDict = dictDestinations.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
                v -> (Cell) v
        );

        return cellDict;
    }

    public static Cell createPendingQueries(List<PendingQuery> pendingQueries, int n) {
        int dictKeySize = 64;
        TonHashMapE dictDestinations = new TonHashMapE(dictKeySize);

        long i = 0; // key, index 16bit
        for (PendingQuery query : pendingQueries) {

            CellBuilder queryCell = CellBuilder.beginCell();
            queryCell.storeBit(true);
            queryCell.storeUint(query.getCreatorI(), 8);
            queryCell.storeUint(query.getCnt(), 8);
            queryCell.storeUint(query.getCntBits(), n);
            queryCell.storeCell(query.getMsg());

            dictDestinations.elements.put(
                    query.getQueryId(), // key - query-id
                    queryCell.endCell() // value - cell - QueryData
            );
        }

        Cell cellDict = dictDestinations.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, dictKeySize).endCell().getBits(),
                v -> (Cell) v
        );

        return cellDict;
    }

    public static Cell createSignaturesDict(List<byte[]> signatures) {
        int dictKeySize = 8; // what is the size of the key?
        TonHashMapE dictSignatures = new TonHashMapE(dictKeySize);

        long i = 0; // key, index
        for (byte[] signature : signatures) {

            CellBuilder sigCell = CellBuilder.beginCell();
            sigCell.storeBytes(signature);
            sigCell.storeUint(i, 8);

            dictSignatures.elements.put(
                    i, // key - index
                    sigCell.endCell() // value - cell - Signature, 512+8
            );
            i++;
        }

        Cell cellDict = dictSignatures.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
                v -> (Cell) v
        );

        return cellDict;
    }

    /**
     * Serialized list of signatures into cell
     *
     * @param i          start index
     * @param signatures list of signatures
     * @return Cell
     */
    public static Cell serializeSignatures(int i, List<MultisigSignature> signatures) {

        CellBuilder c = CellBuilder.beginCell();
        c.storeBytes(signatures.get(i).getSignature());
        c.storeUint(signatures.get(i).getPubKeyPosition(), 8);
        if (i == signatures.size() - 1) {
            c.storeBit(false); // empty dict, last cell
        } else {
            c.storeBit(true);
            c.storeRef(serializeSignatures(++i, signatures));
        }
        return c.endCell();
    }

    public static Cell createQuery(TweetNaclFast.Signature.KeyPair keyPair, List<MultisigSignature> signatures, Cell order) {

        CellBuilder rootCell = CellBuilder.beginCell();
        rootCell.storeUint(0, 8); // root-i
        if (isNull(signatures) || signatures.isEmpty()) {
            rootCell.storeBit(false);
        } else {
            rootCell.storeBit(true);
            rootCell.storeRef(serializeSignatures(0, signatures));
        }
        CellSlice cs = CellSlice.beginParse(order);
        cs.skipBit(); // remove no-signatures flag
        CellBuilder o = CellBuilder.beginCell();
        o.storeCell(cs.sliceToCell());

        rootCell.storeCell(o.endCell());

        byte[] rootSignature = signCell(keyPair, rootCell.endCell());

        CellBuilder query = CellBuilder.beginCell();
        query.storeBytes(rootSignature);
        query.storeCell(rootCell.endCell());

        return query.endCell();
    }

    public ExtMessageInfo deploy() {

        Message externalMessage = Message.builder()
                .info(ExternalMessageInInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .init(getStateInit())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }

    /**
     * @param destination address
     * @param amount      values in nano-tons
     * @param mode        send mode
     * @return Cell
     */
    public static Cell createOneInternalMsg(Address destination, BigInteger amount, int mode) {
        Message internalMessage = Message.builder()
                .info(InternalMessageInfo.builder()
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(destination.wc)
                                .address(destination.toBigInteger())
                                .build())
                        .value(CurrencyCollection.builder().coins(amount).build())
                        .build()).build();

        CellBuilder p = CellBuilder.beginCell();
        p.storeUint(mode, 8);
        p.storeRef(internalMessage.toCell());

        return p.endCell();
    }

    /**
     * @param internalMsgs List of Cells, where Cell is internal msg, defining target destinations with amounts
     * @return Cell Order
     */
    public static Cell createOrder(Long walletId, BigInteger queryId, Cell... internalMsgs) {
        if (internalMsgs.length > 3) {
            throw new Error("Order cannot contain more than 3 internal messages");
        }
        CellBuilder order = CellBuilder.beginCell();
        order.storeBit(false); // no signatures
        order.storeUint(walletId, 32);
        order.storeUint(queryId, 64);

        for (Cell msg : internalMsgs) {
            order.storeCell(msg);
        }
        return order.endCell();
    }

    public static Cell createOrder1(Long walletId, BigInteger queryId, Cell... internalMsgs) {
        if (internalMsgs.length > 3) {
            throw new Error("Order cannot contain more than 3 internal messages");
        }
        CellBuilder order = CellBuilder.beginCell();
        order.storeUint(walletId, 32);
        order.storeUint(queryId, 64);

        for (Cell msg : internalMsgs) {
            order.storeCell(msg);
        }
        return order.endCell();
    }

    public static Cell addSignatures(Cell order, List<MultisigSignature> signatures) {

        CellBuilder signedOrder = CellBuilder.beginCell();
        signedOrder.storeBit(true); // contains signatures
        signedOrder.storeRef(serializeSignatures(0, signatures));

        CellSlice cs = CellSlice.beginParse(order);
        cs.skipBit(); // remove no-signatures flag
        CellBuilder o = CellBuilder.beginCell();
        o.storeCell(cs.sliceToCell());

        signedOrder.storeCell(o.endCell());
        return signedOrder.endCell();
    }

    private static void checkIfSignatureExists(Cell order, byte[] signature) {
        CellSlice cs = CellSlice.beginParse(order);

        if (cs.loadBit()) { //order contains signatures
            Cell ref = cs.loadRef();
            while (nonNull(ref)) {
                byte[] sig = CellSlice.beginParse(ref).loadBytes(512);
                log.info("sig " + Utils.bytesToHex(signature));
                if (sig == signature) {
                    throw new Error("Your signature is already presented");
                }
                if (ref.getUsedRefs() != 0) {
                    ref = ref.getRefs().get(0);
                } else {
                    ref = null;
                }
            }
        }
    }

    public static Cell addSignature1(Cell order, int pubkeyIndex, TweetNaclFast.Signature.KeyPair keyPair) {

        CellSlice cs = CellSlice.beginParse(order);
        cs.skipBit(); // remove no-signatures flag
        CellBuilder o = CellBuilder.beginCell();
        o.storeCell(cs.sliceToCell());

        byte[] signature = signCell(keyPair, o.endCell());

        log.info("sig " + Utils.bytesToHex(signature));

//        checkIfSignatureExists(order, signature);

        cs = CellSlice.beginParse(order);
        if (!cs.loadBit()) { //order didn't have any signatures, add first signature
            cs.skipBit(); // remove no-signatures flag

            CellBuilder signedOrder = CellBuilder.beginCell();
            signedOrder.storeBit(true); // contains signatures

            CellBuilder c = CellBuilder.beginCell();
            c.storeBytes(signature);
            c.storeUint(pubkeyIndex, 8);
            c.storeBit(false); // no more references, only one signature added

            signedOrder.storeRef(c.endCell());
            signedOrder.storeCell(o.endCell());
            return signedOrder.endCell();
        } else { // order contains some signatures
            Cell otherSignatures = cs.loadRef();

            CellBuilder signedOrder = CellBuilder.beginCell();
            signedOrder.storeBit(true); // contains signatures

            CellBuilder c = CellBuilder.beginCell();
            c.storeBytes(signature); // add new signature
            c.storeUint(pubkeyIndex, 8);
            c.storeBit(true); // add other signatures
            c.storeRef(otherSignatures);

            signedOrder.storeRef(c.endCell());
            signedOrder.storeCell(o.endCell());
            return signedOrder.endCell();
        }
    }


    public static byte[] signCell(TweetNaclFast.Signature.KeyPair keyPair, Cell cell) {
        return new TweetNaclFast.Signature(keyPair.getPublicKey(), keyPair.getSecretKey()).detached(cell.hash());
    }

    public static byte[] signOrder(TweetNaclFast.Signature.KeyPair keyPair, Cell order) {
        CellSlice cs = CellSlice.beginParse(order);
        cs.skipBit(); // remove no-signatures flag
        CellBuilder o = CellBuilder.beginCell();
        o.storeCell(cs.sliceToCell());

        return signCell(keyPair, o.endCell());
    }

    public Pair<Long, Long> getNandK() {

        Address myAddress = getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_n_k");

        if (result.getExit_code() != 0) {
            throw new Error("method get_n_k, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber nNumber = (TvmStackEntryNumber) result.getStack().get(0);
        TvmStackEntryNumber kNumber = (TvmStackEntryNumber) result.getStack().get(1);

        return Pair.of(nNumber.getNumber().longValue(), kNumber.getNumber().longValue());
    }

    /**
     * Returns list of all unsigned messages
     *
     * @return List<Cell> pending queries
     */
    public Map<BigInteger, Cell> getMessagesUnsigned() {

        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_messages_unsigned");

        if (result.getExit_code() != 0) {
            throw new Error("method get_messages_unsigned, returned an exit code " + result.getExit_code());
        }

        if (result.getStack().get(0) instanceof TvmStackEntryList) {
            return new HashMap<>();
        }

        TvmStackEntryCell entryCell = (TvmStackEntryCell) result.getStack().get(0);
        Cell cellDict = CellBuilder.beginCell().fromBocBase64(entryCell.getCell().getBytes()).endCell();

        CellSlice cs = CellSlice.beginParse(cellDict);

        TonHashMap loadedDict = cs
                .loadDict(64,
                        k -> k.readUint(64),
                        v -> v
                );

        Map<BigInteger, Cell> resultMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : loadedDict.elements.entrySet()) {
            // query-id, query
            resultMap.put((BigInteger) entry.getKey(), (Cell) entry.getValue());
        }

        return resultMap;
    }


    /**
     * Returns list of all signed messages by index
     *
     * @return List<Cell> pending queries
     */
    public Map<BigInteger, Cell> getMessagesSignedByIndex(long index) {

        Address myAddress = this.getAddress();
        Deque<String> stack = new ArrayDeque<>();

        stack.offer("[num, " + index + "]");

        RunResult result = tonlib.runMethod(myAddress, "get_messages_signed_by_id", stack);

        if (result.getExit_code() != 0) {
            throw new Error("method get_messages_signed_by_id, returned an exit code " + result.getExit_code());
        }

        if (result.getStack().get(0) instanceof TvmStackEntryList) {
            return new HashMap<>();
        }

        TvmStackEntryCell entryCell = (TvmStackEntryCell) result.getStack().get(0);
        Cell cellDict = CellBuilder.beginCell().fromBocBase64(entryCell.getCell().getBytes()).endCell();

        CellSlice cs = CellSlice.beginParse(cellDict);

        TonHashMap loadedDict = cs
                .loadDict(64,
                        k -> k.readUint(64),
                        v -> v
                );

        Map<BigInteger, Cell> resultMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : loadedDict.elements.entrySet()) {
            // query-id, query
            resultMap.put((BigInteger) entry.getKey(), (Cell) entry.getValue());
        }
        return resultMap;
    }


    /**
     * Returns list of all unsigned messages by index
     *
     * @return List<Cell> pending queries
     */
    public Map<BigInteger, Cell> getMessagesUnsignedByIndex(long index) {

        Address myAddress = this.getAddress();
        Deque<String> stack = new ArrayDeque<>();

        stack.offer("[num, " + index + "]");

        RunResult result = tonlib.runMethod(myAddress, "get_messages_unsigned_by_id", stack);

        if (result.getExit_code() != 0) {
            throw new Error("method get_messages_unsigned_by_id, returned an exit code " + result.getExit_code());
        }

        if (result.getStack().get(0) instanceof TvmStackEntryList) {
            return new HashMap<>();
        }

        TvmStackEntryCell entryCell = (TvmStackEntryCell) result.getStack().get(0);
        Cell cellDict = CellBuilder.beginCell().fromBocBase64(entryCell.getCell().getBytes()).endCell();

        CellSlice cs = CellSlice.beginParse(cellDict);

        TonHashMap loadedDict = cs
                .loadDict(64,
                        k -> k.readUint(64),
                        v -> v
                );

        Map<BigInteger, Cell> resultMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : loadedDict.elements.entrySet()) {
            // query-id, query
            resultMap.put((BigInteger) entry.getKey(), (Cell) entry.getValue());
        }
        return resultMap;
    }

    /**
     * Returns -1 for processed queries, 0 for unprocessed, 1 for unknown (forgotten)
     * and the mask of signed positions of pubkeys
     *
     * @return Pair<Long, Long> status, mask
     */
    public Pair<Long, Long> getQueryState(BigInteger queryId) {

        Address myAddress = this.getAddress();

        Deque<String> stack = new ArrayDeque<>();

        stack.offer("[num, " + queryId.toString(10) + "]");

        RunResult result = tonlib.runMethod(myAddress, "get_query_state", stack);

        if (result.getExit_code() != 0) {
            throw new Error("method get_query_state, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber r = (TvmStackEntryNumber) result.getStack().get(0);
        TvmStackEntryNumber n = (TvmStackEntryNumber) result.getStack().get(1);
        return Pair.of(r.getNumber().longValue(), n.getNumber().longValue());
    }

    /**
     * You can check whether signatures used to sign the order are correct
     *
     * @param tonlib Tonlib
     * @param query  Cell of serialized list of signatures and order
     * @return Pair<Long, Long> count of correct signatures and the mask
     */
    public Pair<Long, Long> checkQuerySignatures(Tonlib tonlib, Cell query) {

        Address myAddress = this.getAddress();
        Deque<String> stack = new ArrayDeque<>();

        stack.offer("[cell, " + query.toHex(false) + "]");
        RunResult result = tonlib.runMethod(myAddress, "check_query_signatures", stack);

        if (result.getExit_code() != 0) {
            throw new Error("method check_query_signatures, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber cnt = (TvmStackEntryNumber) result.getStack().get(0);
        TvmStackEntryNumber mask = (TvmStackEntryNumber) result.getStack().get(1);

        return Pair.of(cnt.getNumber().longValue(), mask.getNumber().longValue());
    }

    public Cell mergePendingQueries(Tonlib tonlib, Cell a, Cell b) {

        Address myAddress = this.getAddress();
        Deque<String> stack = new ArrayDeque<>();

        stack.offer("[cell, " + a.toHex(false) + "]");
        stack.offer("[cell, " + b.toHex(false) + "]");
        RunResult result = tonlib.runMethod(myAddress, "merge_inner_queries", stack);

        if (result.getExit_code() != 0) {
            throw new Error("method merge_inner_queries, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryCell entryCell = (TvmStackEntryCell) result.getStack().get(0);

        return CellBuilder.beginCell().fromBocBase64(entryCell.getCell().getBytes()).endCell();
    }

    /**
     * Returns -1 for processed queries, 0 for unprocessed, 1 for unknown (forgotten)
     *
     * @return Long status
     */
    public long processed(Tonlib tonlib, BigInteger queryId) {

        Address myAddress = this.getAddress();

        Deque<String> stack = new ArrayDeque<>();

        stack.offer("[num, " + queryId.toString(10) + "]");

        RunResult result = tonlib.runMethod(myAddress, "processed?", stack);

        if (result.getExit_code() != 0) {
            throw new Error("method processed, returned an exit code " + result.getExit_code());
        }
        TvmStackEntryNumber cnt = (TvmStackEntryNumber) result.getStack().get(0);
        return cnt.getNumber().longValue();
    }
}