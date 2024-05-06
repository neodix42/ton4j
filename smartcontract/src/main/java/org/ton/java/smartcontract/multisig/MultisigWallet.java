package org.ton.java.smartcontract.multisig;

import com.iwebpp.crypto.TweetNaclFast;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.address.Address;
import org.ton.java.cell.*;
import org.ton.java.smartcontract.types.*;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tlb.types.ExternalMessageInfo;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class MultisigWallet implements Contract<MultisigWalletConfig> {

    //https://github.com/akifoq/multisig/blob/master/multisig-code.fc
    Options options;
    Address address;

    /**
     * interface to <a href="https://github.com/akifoq/multisig/blob/master/multisig-code.fc">multisig wallet smart-contract</a>
     *
     * @param options Options - mandatory -  highloadQueryId, walletId, publicKey
     */
    public MultisigWallet(Options options) {
        this.options = options;
        options.code = CellBuilder.beginCell().fromBoc(WalletCodes.multisig.getValue()).endCell();
    }

    @Override
    public String getName() {
        return "multisig";
    }

    @Override
    public Options getOptions() {
        return options;
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

        cell.storeUint(getOptions().getWalletId(), 32);
        cell.storeUint(getOptions().getMultisigConfig().getN(), 8); // n
        cell.storeUint(getOptions().getMultisigConfig().getK(), 8); // k - collect at least k signatures
        cell.storeUint(0, 64); // last cleaned
        if (isNull(getOptions().getMultisigConfig().getOwners()) || getOptions().getMultisigConfig().getOwners().isEmpty()) {
            cell.storeBit(false); // initial owners dict
        } else {
            cell.storeDict(createOwnersInfosDict(getOptions().getMultisigConfig().getOwners()));
        }

        if (isNull(getOptions().getMultisigConfig().getPendingQueries()) || getOptions().getMultisigConfig().getPendingQueries().isEmpty()) {
            cell.storeBit(false); // initial  pending queries dict
        } else {
            cell.storeDict(createPendingQueries(getOptions().getMultisigConfig().getPendingQueries(), getOptions().getMultisigConfig().getN()));
        }

        return cell.endCell();
    }

    private Cell createSigningMessageInternal(int pubkeyIndex, Cell order) {
        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(pubkeyIndex, 8); // root-id - pk-index for owner_infos dict
        message.storeCell(order);
        return message.endCell();
    }


    public List<BigInteger> getPublicKeys(Tonlib tonlib) {

        List<BigInteger> publicKeys = new ArrayList<>();

        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_public_keys");

        if (result.getExit_code() != 0) {
            throw new Error("method get_public_keys, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryCell cellResult = (TvmStackEntryCell) result.getStack().get(0);
        Cell cell = CellBuilder.beginCell().fromBoc(cellResult.getCell().getBytes()).endCell();

        CellSlice cs = CellSlice.beginParse(cell);
        TonHashMap loadedDict = cs.loadDict(8,
                k -> k.readUint(8), // index
                v -> v // ownerInfo cell
        );
        for (Map.Entry<Object, Object> entry : loadedDict.elements.entrySet()) {
            CellSlice cSlice = CellSlice.beginParse((Cell) entry.getValue());
            BigInteger pubKey = cSlice.loadUint(256);
            long flood = cSlice.loadUint(8).longValue();
            publicKeys.add(pubKey);
        }
        return publicKeys;
    }

    public List<String> getPublicKeysHex(Tonlib tonlib) {
        List<BigInteger> l = getPublicKeys(tonlib);
        List<String> result = new ArrayList<>();
        for (BigInteger i : l) {
            result.add(i.toString(16));
        }
        return result;
    }

    /**
     * generates and returns init-state onchain
     *
     * @param tonlib     tonlib
     * @param walletId   walletid
     * @param n          total keys
     * @param k          minimum number of keys
     * @param ownersInfo arrays with public keys
     * @return cell with state-init
     */
    public Cell getInitState(Tonlib tonlib, long walletId, int n, int k, Cell ownersInfo) {

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
        return CellBuilder.beginCell().fromBoc(domainCell.getCell().getBytes()).endCell();
    }

    /**
     * Sends an external msg with the order containing all collected signatures signed by owner at index pubkeyIndex with keyPair.
     *
     * @param tonlib  Tonlib
     * @param keyPair TweetNaclFast.Signature.KeyPair
     */
    public void sendOrder(Tonlib tonlib, TweetNaclFast.Signature.KeyPair keyPair, int pubkeyIndex, Cell order) {
//        Cell signingMessageBody = createSigningMessageInternal(pubkeyIndex, order);
//        ExternalMessage msg = createExternalMessage(signingMessageBody, keyPair.getSecretKey(), 1, false);
//        tonlib.sendRawMessage(msg.message.toBase64());
    }

    /**
     * Sends an external msg with the order containing all collected signatures signed by owner at index pubkeyIndex with secretKey.
     *
     * @param tonlib    Tonlib
     * @param secretKey byte[]
     */
    public void sendOrder(Tonlib tonlib, byte[] secretKey, int pubkeyIndex, Cell order) {
//        Cell signingMessageBody = createSigningMessageInternal(pubkeyIndex, order);
//        ExternalMessage msg = createExternalMessage(signingMessageBody, secretKey, 1, false);
//        tonlib.sendRawMessage(msg.message.toBase64());
    }

    /**
     * Serializes list of multisig wallet owners.
     *
     * @param ownerInfos OwnerInfo
     * @return Cell
     */
    public Cell createOwnersInfosDict(List<OwnerInfo> ownerInfos) {
        int dictKeySize = 8;
        TonHashMapE dictDestinations = new TonHashMapE(dictKeySize);

        long i = 0; // key, index 16bit
        for (OwnerInfo ownerInfo : ownerInfos) {

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

    /**
     * We do not override createSigningMessage() since we can send an empty message for deployment.
     */
//    public ExtMessageInfo deploy(Tonlib tonlib, byte[] secretKey) {
//        return tonlib.sendRawMessage(createInitExternalMessageWithoutBody(secretKey).message.toBase64());
//    }
    @Override
    public Cell createTransferBody(MultisigWalletConfig config) {
        return CellBuilder.beginCell().endCell();
    }

    @Override
    public ExtMessageInfo deploy(Tonlib tonlib, MultisigWalletConfig config) {

        Cell body = createTransferBody(config);
//
        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .init(createStateInit())
//                .body(body)
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(getOptions().getPublicKey(), getOptions().getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
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
//        Cell intMsgHeader = Contract.createInternalMessageHeader(destination, amount);
//        Cell intMsgTransfer = Contract.createCommonMsgInfo(intMsgHeader);
//
//        CellBuilder p = CellBuilder.beginCell();
//        p.storeUint(mode, 8);
//        p.storeRef(intMsgTransfer);
//
//        return p.endCell();
        return null; // todo
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
                System.out.println("sig " + Utils.bytesToHex(signature));
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

        System.out.println("sig " + Utils.bytesToHex(signature));

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

    public Pair<Long, Long> getNandK(Tonlib tonlib) {

        Address myAddress = this.getAddress();
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
     * @param tonlib Tonlib
     * @return List<Cell> pending queries
     */
    public Map<BigInteger, Cell> getMessagesUnsigned(Tonlib tonlib) {

        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_messages_unsigned");

        if (result.getExit_code() != 0) {
            throw new Error("method get_messages_unsigned, returned an exit code " + result.getExit_code());
        }

        if (result.getStack().get(0) instanceof TvmStackEntryList) {
            return new HashMap<>();
        }

        TvmStackEntryCell entryCell = (TvmStackEntryCell) result.getStack().get(0);
        Cell cellDict = CellBuilder.beginCell().fromBoc(entryCell.getCell().getBytes()).endCell();

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
     * @param tonlib Tonlib
     * @return List<Cell> pending queries
     */
    public Map<BigInteger, Cell> getMessagesSignedByIndex(Tonlib tonlib, long index) {

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
        Cell cellDict = CellBuilder.beginCell().fromBoc(entryCell.getCell().getBytes()).endCell();

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
     * @param tonlib Tonlib
     * @return List<Cell> pending queries
     */
    public Map<BigInteger, Cell> getMessagesUnsignedByIndex(Tonlib tonlib, long index) {

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
        Cell cellDict = CellBuilder.beginCell().fromBoc(entryCell.getCell().getBytes()).endCell();

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
    public Pair<Long, Long> getQueryState(Tonlib tonlib, BigInteger queryId) {

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

        return CellBuilder.beginCell().fromBoc(entryCell.getCell().getBytes()).endCell();
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