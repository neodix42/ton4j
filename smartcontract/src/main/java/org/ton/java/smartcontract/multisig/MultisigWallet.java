package org.ton.java.smartcontract.multisig;

import com.iwebpp.crypto.TweetNaclFast;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.address.Address;
import org.ton.java.cell.*;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.OwnerInfo;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryCell;
import org.ton.java.tonlib.types.TvmStackEntryList;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.*;

public class MultisigWallet implements WalletContract {

    //https://github.com/akifoq/multisig/blob/master/multisig-code.fc
    public static final String MULTISIG_WALLET_CODE_HEX = "B5EE9C7241022B0100041A000114FF00F4A413F4BCF2C80B010201200203020148040504DAF220C7008E8330DB3CE08308D71820F90101D307DB3C22C00013A1537178F40E6FA1F29FDB3C541ABAF910F2A006F40420F90101D31F5118BAF2AAD33F705301F00A01C20801830ABCB1F26853158040F40E6FA120980EA420C20AF2670EDFF823AA1F5340B9F2615423A3534E202321220202CC06070201200C0D02012008090201660A0B0003D1840223F2980BC7A0737D0986D9E52ED9E013C7A21C2125002D00A908B5D244A824C8B5D2A5C0B5007404FC02BA1B04A0004F085BA44C78081BA44C3800740835D2B0C026B500BC02F21633C5B332781C75C8F20073C5BD0032600201200E0F02012014150115BBED96D5034705520DB3C82A020148101102012012130173B11D7420C235C6083E404074C1E08075313B50F614C81E3D039BE87CA7F5C2FFD78C7E443CA82B807D01085BA4D6DC4CB83E405636CF0069006027003DAEDA80E800E800FA02017A0211FC8080FC80DD794FF805E47A0000E78B64C00019AE19573859C100D56676A1EC40020120161702012018190151B7255B678626466A4610081E81CDF431C24D845A4000331A61E62E005AE0261C0B6FEE1C0B77746E10230189B5599B6786ABE06FEDB1C6CA2270081E8F8DF4A411C4A05A400031C38410021AE424BAE064F6451613990039E2CA840090081E886052261C52261C52265C4036625CCD8A30230201201A1B0017B506B5CE104035599DA87B100201201C1D020399381E1F0111AC1A6D9E2F81B60940230015ADF94100CC9576A1EC1840010DA936CF0557C160230017ADDC2CDC20806AB33B50F6200220DB3C02F265F8005043714313DB3CED54232A000AD3FFD3073004A0DB3C2FAE5320B0F26212B102A425B3531CB9B0258100E1AA23A028BCB0F269820186A0F8010597021110023E3E308E8D11101FDB3C40D778F44310BD05E254165B5473E7561053DCDB3C54710A547ABC242528260020ED44D0D31FD307D307D33FF404F404D1005E018E1A30D20001F2A3D307D3075003D70120F90105F90115BAF2A45003E06C2121D74AAA0222D749BAF2AB70542013000C01C8CBFFCB0704D6DB3CED54F80F70256E5389BEB198106E102D50C75F078F1B30542403504DDB3C5055A046501049103A4B0953B9DB3C5054167FE2F800078325A18E2C268040F4966FA52094305303B9DE208E1638393908D2000197D3073016F007059130E27F080705926C31E2B3E630062A2728290060708E2903D08308D718D307F40430531678F40E6FA1F2A5D70BFF544544F910F2A6AE5220B15203BD14A1236EE66C2232007E5230BE8E205F03F8009322D74A9802D307D402FB0002E83270C8CA0040148040F44302F0078E1771C8CB0014CB0712CB0758CF0158CF1640138040F44301E201208E8A104510344300DB3CED54925F06E22A001CC8CB1FCB07CB07CB3FF400F400C984B5AC4C";
    Options options;
    Address address;

    /**
     * interface to <a href="https://github.com/akifoq/multisig/blob/master/multisig-code.fc">multisig wallet smart-contract</a>
     *
     * @param options Options - mandatory -  highloadQueryId, walletId, publicKey
     */
    public MultisigWallet(Options options) {
        this.options = options;
        options.code = Cell.fromBoc(MULTISIG_WALLET_CODE_HEX);
    }

    @Override
    public String getName() {
        return "multisig";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public Address getAddress() {
        if (address == null) {
            return (createStateInit()).address;
        }
        return address;
    }

    /**
     * Initial contract storage (init state).
     * Creator/deployer will be always part of k signatures.
     * By default, it will reside in owner_infos dict at index 0.
     *
     * @return cell Cell
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();

        cell.storeUint(getOptions().getWalletId(), 32); // sub-wallet id
        cell.storeUint(getOptions().getMultisigConfig().getN(), 8); // n
        cell.storeUint(getOptions().getMultisigConfig().getK(), 8); // k - collect at least k signatures
        cell.storeUint(BigInteger.ZERO, 64); // last cleaned
        cell.storeDict(createOwnersInfosDict(getOptions().getMultisigConfig().getOwners())); // initial owner infos dict, public keys
        cell.storeBit(false); // initial  pending queries dict

        return cell.endCell();
    }

    public Cell createSigningMessageInternal(TweetNaclFast.Signature.KeyPair keyPair, List<byte[]> signatures, Cell signedOrder) {

        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(getOptions().getMultisigConfig().getRootI(), 8); // root-id - pk-index for owner_infos dict

//        message.storeBit(false); // no dict - works ok
        message.storeBit(true); // sigs dict exists and not empty - works ok

        message.storeUint(getOptions().getWalletId(), 32); // wallet-id
        message.storeUint(getOptions().getMultisigConfig().getQueryId(), 64); // query-id

        Cell sigsDict = createSignaturesRecursiveCell(keyPair, signatures, signedOrder);
        message.storeRef(sigsDict); // works

        Cell msg = createOneInternalMsg(Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"), Utils.toNano(0.5), 3);
        message.writeCell(msg);

        // orders add here?
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
        Cell cell = CellBuilder.fromBoc(Utils.base64ToBytes(cellResult.getCell().getBytes()));

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
     * @param n          - total keys
     * @param k          - minimum number of keys
     * @param ownersInfo - arrays with public keys
     * @return cell with state-init
     */
    public Cell getInitState(Tonlib tonlib, int walletId, int n, int k, Cell ownersInfo) {

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
        return CellBuilder.fromBoc(Utils.base64ToBytes(domainCell.getCell().getBytes()));
    }

    /**
     * Sends to up to 84 destinations
     *
     * @param tonlib     Tonlib
     * @param keyPair    TweetNaclFast.Signature.KeyPair
     * @param signatures List<byte[]>
     */
    public void sendSignedQuery(Tonlib tonlib, TweetNaclFast.Signature.KeyPair keyPair, Cell signedOrder, List<byte[]> signatures) {

        Cell signingMessageBody = createSigningMessageInternal(keyPair, signatures, signedOrder);

        ExternalMessage msg = createExternalMessage(signingMessageBody, keyPair.getSecretKey(), 1, false);

        tonlib.sendRawMessage(msg.message.toBocBase64(false));
    }

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
                k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).bits,
                v -> (Cell) v
        );

        return cellDict;
    }

    public Cell createQuery(TweetNaclFast.Signature.KeyPair keyPair, List<byte[]> signatures, Cell order) {

        byte[] signatureOrder = signCellHash(keyPair, order);

        CellBuilder sig1 = CellBuilder.beginCell();
        sig1.storeUint(1, 8); // lets reuse same pubkey
        sig1.storeBit(false); // empty dict sigs

//        byte[] signature1 = getOrderSignature(keyPair, sig1.endCell());

        CellBuilder signedOrder1 = CellBuilder.beginCell();
        signedOrder1.storeBytes(signatures.get(1)); // all sub cell apart root will contain hash of top order
        signedOrder1.writeCell(sig1);

        CellBuilder rootCell = CellBuilder.beginCell();
        rootCell.storeUint(0, 8); // root-i
        rootCell.storeBit(true); // not empty dict
        rootCell.storeRef(signedOrder1);
        rootCell.writeCell(order);

        byte[] rootSignature = signCellHash(keyPair, rootCell.endCell());

        // todo check if our signature already exist inside order

        CellBuilder query = CellBuilder.beginCell();
        query.storeBytes(rootSignature);
        query.writeCell(rootCell);

        return query;
    }

    public Cell createSignaturesRecursiveCell(TweetNaclFast.Signature.KeyPair keyPair, List<byte[]> signatures, Cell order) {

        CellBuilder dummy = CellBuilder.beginCell();
        dummy.storeUint(0, 8);
        dummy.storeBit(false);

        byte[] signature = signCellHash(keyPair, dummy);

        // todo check if our signature already exist inside order

        CellBuilder signedOrder = CellBuilder.beginCell();
        signedOrder.storeBytes(signature);
        signedOrder.writeCell(dummy);


        return signedOrder;
        /*
        long i = 0; // key, index 8bit

        CellBuilder sigsCell = CellBuilder.beginCell();
        for (byte[] signature : signatures) {

            sigsCell.storeBytes(signature); //512 bits -- works
//            sigsCell.storeUint(i, 8);
//            sigsCell.storeBit(false);

            sigsCell.writeCell(order);

//            sigsCell.storeRef(CellBuilder.beginCell().endCell());
            i++;
        }

        return sigsCell;

         */
    }

    /**
     * We do not override createSigningMessage() since we can send an empty message for deployment.
     *
     * @param tonlib    Tonlib
     * @param secretKey secret key
     */
    public void deploy(Tonlib tonlib, byte[] secretKey) {
        tonlib.sendRawMessage(createInitExternalMessageWithoutBody(secretKey).message.toBocBase64(false));
    }

    /**
     * @param destination address
     * @param amount      values in nano-tons
     * @param mode        send mode
     * @return Cell
     */
    public Cell createOneInternalMsg(Address destination, BigInteger amount, int mode) {
        Cell orderHeader = Contract.createInternalMessageHeader(destination, amount);
        Cell transferMessage = Contract.createCommonMsgInfo(orderHeader);

        CellBuilder p = CellBuilder.beginCell();
        p.storeUint(mode, 8);
        p.storeRef(transferMessage);

        return p.endCell();
    }

    /**
     * @param walletId     sub-wallet-id
     * @param queryId      time-out
     * @param internalMsgs List of Cells, where Cell is internal msg, defining target destinations with amounts
     * @return Cell
     */
    public Cell createOrder(long walletId, BigInteger queryId, List<Cell> internalMsgs) {
        CellBuilder order = CellBuilder.beginCell();
        order.storeBit(false); // no signatures
        // in fift we need it
//        order.storeUint(walletId, 32);
//        order.storeUint(queryId, 64); // timeout also

        for (Cell msg : internalMsgs) { // "N must be in range 1..3" todo
            order.storeRef(msg); // not ref in fift - https://github.com/akifoq/multisig/blob/master/create-order.fif
        }
        return order.endCell();
    }

    public Cell addInternalMsgToOrder() {
        return null;
    }

    /**
     * Signs the order with private key corresponding to public at position pubKeyIndex in OwnersDict
     *
     * @param keyPair
     * @param pubKeyIndex
     * @param order
     * @return Cell
     */
    public Cell signOrder(TweetNaclFast.Signature.KeyPair keyPair, int pubKeyIndex, Cell order) {
        byte[] signature = signCellHash(keyPair, order);

        CellSlice cs = CellSlice.beginParse(order);

        // todo check if our signature already exist inside order

        CellBuilder signedOrder = CellBuilder.beginCell();
        signedOrder.storeBytes(signature);
        signedOrder.storeUint(pubKeyIndex, 8);

        if (cs.loadBit()) {
            //if null .writeBit(0) ?
            signedOrder.storeBit(true);
            signedOrder.storeRef(CellSlice.beginParse(order).loadRef()); // load int msg = list of sigs!
        } else {
            signedOrder.storeBit(false); // list of sigs is empty
        }

        //signedOrder.writeCell(order); // todo two times

        CellBuilder signedOrderMsg = CellBuilder.beginCell();
        signedOrderMsg.storeInt(-1, 1);
//        signedOrderMsg.storeBit(true);
        signedOrderMsg.storeRef(signedOrder.endCell());
        signedOrderMsg.writeCell(order); // todo two times

        return signedOrderMsg;
    }

    public byte[] signCellHash(TweetNaclFast.Signature.KeyPair keyPair, Cell cell) {
        return new TweetNaclFast.Signature(keyPair.getPublicKey(), keyPair.getSecretKey()).detached(cell.hash());
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
//        TvmStackEntryList entryList = (TvmStackEntryList) result.getStack().get(0);
//        System.out.println(entryList);

        TvmStackEntryCell entryCell = (TvmStackEntryCell) result.getStack().get(0);
        Cell cellDict = CellBuilder.fromBoc(Utils.base64ToBytes(entryCell.getCell().getBytes()));

        // returns dict <64bits, cell query-data>
//        query-data
//             .store_uint(1, 1)
//             .store_uint(creator_i, 8)
//             .store_uint(cnt, 8)
//             .store_uint(cnt_bits, n)
//             .store_slice(msg));

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
     * returns -1 for processed queries, 0 for unprocessed, 1 for unknown (forgotten)
     *
     * @return Pair<Long, Long>
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
}

//query
//              .store_uint(1, 1)
//             .store_uint(creator_i, 8)
//             .store_uint(cnt, 8)
//             .store_uint(cnt_bits, n)
//             .store_slice(msg));