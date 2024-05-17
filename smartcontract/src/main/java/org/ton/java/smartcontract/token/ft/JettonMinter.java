package org.ton.java.smartcontract.token.ft;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.JettonMinterConfig;
import org.ton.java.smartcontract.types.JettonMinterData;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.v3.WalletV3ContractR1;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class JettonMinter implements Contract<JettonMinterConfig> {

    Options options;
    Address address;

    /**
     * @param options Options
     */
    public JettonMinter(Options options) {
        this.options = options;
        this.options.wc = 0;

        if (nonNull(options.address)) {
            this.address = Address.of(options.address);
        }
        if (isNull(options.code)) {
            options.code = CellBuilder.beginCell().fromBoc(WalletCodes.jettonMinter.getValue()).endCell();
        }
    }

    public String getName() {
        return "jettonMinter";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    /**
     * @return Cell cell - contains jetton data cell
     */
    @Override
    public Cell createDataCell() {
        return CellBuilder.beginCell()
                .storeCoins(BigInteger.ZERO)
                .storeAddress(options.adminAddress)
                .storeRef(NftUtils.createOffchainUriCell(options.jettonContentUri))
                .storeRef(CellBuilder.beginCell().fromBoc(options.jettonWalletCodeHex).endCell())
                .endCell();
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.jettonMinter.getValue()).
                endCell();
    }

    public Cell createTransferBody(JettonMinterConfig config) {
        Address ownAddress = getAddress();
        CommonMsgInfo internalMsgInfo = InternalMessageInfo.builder()
                .srcAddr(MsgAddressIntStd.builder()
                        .workchainId(ownAddress.wc)
                        .address(ownAddress.toBigInteger())
                        .build())
                .dstAddr(MsgAddressIntStd.builder()
                        .workchainId(config.getDestination().wc)
                        .address(config.getDestination().toBigInteger())
                        .build())
                .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                .createdAt(config.getCreatedAt())
                .build();

        Cell innerMsg = internalMsgInfo.toCell();

        Cell order = Message.builder()
                .info(internalMsgInfo)
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(getOptions().publicKey, options.getSecretKey(), innerMsg.hash()))
                        .storeRef(innerMsg)
                        .endCell())
                .build().toCell();

        return CellBuilder.beginCell()
                .storeUint(BigInteger.valueOf(config.getSeqno()), 32)
                .storeUint(config.getMode() & 0xff, 8)
                .storeRef(order)
                .endCell();
    }

    /**
     * @param queryId      long
     * @param destination  Address
     * @param amount       BigInteger
     * @param jettonAmount BigInteger
     * @return Cell
     */
    public Cell createMintBody(long queryId, Address destination, BigInteger amount, BigInteger jettonAmount) {

        return createMintBody(queryId, destination, amount, jettonAmount,
                null, null,
                BigInteger.ZERO);
    }

    /**
     * @param queryId         long
     * @param destination     Address
     * @param amount          BigInteger
     * @param jettonAmount    BigInteger
     * @param fromAddress     Address
     * @param responseAddress Address
     * @param forwardAmount   BigInteger
     * @return Cell
     */
    public Cell createMintBody(long queryId, Address destination, BigInteger amount,
                               BigInteger jettonAmount, Address fromAddress, Address
                                       responseAddress, BigInteger forwardAmount) {
        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(21, 32); // OP mint
        body.storeUint(queryId, 64);   // query_id, default 0
        body.storeAddress(destination);
        body.storeCoins(amount);

        CellBuilder transferBody = CellBuilder.beginCell(); // internal transfer
        transferBody.storeUint(0x178d4519, 32); // internal_transfer op
        transferBody.storeUint(queryId, 64); // default 0
        transferBody.storeCoins(jettonAmount);
        transferBody.storeAddress(fromAddress);     // from_address
        transferBody.storeAddress(responseAddress); // response_address
        transferBody.storeCoins(forwardAmount);     // forward_amount
        transferBody.storeBit(false); // forward_payload in this slice, not separate cell

        body.storeRef(transferBody.endCell());

        return body.endCell();
    }

    /**
     * @param queryId         long
     * @param newAdminAddress Address
     * @return Cell
     */
    public Cell createChangeAdminBody(long queryId, Address newAdminAddress) {
        if (isNull(newAdminAddress)) {
            throw new Error("Specify newAdminAddress");
        }

        return CellBuilder.beginCell()
                .storeUint(3, 32) // OP
                .storeUint(queryId, 64) // query_id
                .storeAddress(newAdminAddress)
                .endCell();
    }

    /**
     * @param jettonContentUri: String
     * @param queryId           long
     * @return Cell
     */
    public Cell createEditContentBody(String jettonContentUri, long queryId) {
        return CellBuilder.beginCell()
                .storeUint(4, 32) // OP change content
                .storeUint(queryId, 64) // query_id
                .storeRef(NftUtils.createOffchainUriCell(jettonContentUri))
                .endCell();
    }

    /**
     * @return JettonData
     */
    public JettonMinterData getJettonData(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_jetton_data"); //minter

        if (result.getExit_code() != 0) {
            throw new Error("method get_nft_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber totalSupplyNumber = (TvmStackEntryNumber) result.getStack().get(0);
        BigInteger totalSupply = totalSupplyNumber.getNumber();

        System.out.println("minter totalSupply: " + Utils.formatNanoValue(totalSupply));

        boolean isMutable = ((TvmStackEntryNumber) result.getStack().get(1)).getNumber().longValue() == -1;

        TvmStackEntrySlice adminAddr = (TvmStackEntrySlice) result.getStack().get(2);
        Address adminAddress = NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(adminAddr.getSlice().getBytes())).endCell());

        TvmStackEntryCell jettonContent = (TvmStackEntryCell) result.getStack().get(3);
        Cell jettonContentCell = CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(jettonContent.getCell().getBytes())).endCell();
        String jettonContentUri = null;
        try {
            jettonContentUri = NftUtils.parseOffchainUriCell(jettonContentCell);
        } catch (Exception e) {
            //todo
        }

        TvmStackEntryCell contentC = (TvmStackEntryCell) result.getStack().get(4);
        Cell jettonWalletCode = CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(contentC.getCell().getBytes())).endCell();

        return JettonMinterData.builder()
                .totalSupply(totalSupply)
                .isMutable(isMutable)
                .adminAddress(adminAddress)
                .jettonContentCell(jettonContentCell)
                .jettonContentUri(jettonContentUri)
                .jettonWalletCode(jettonWalletCode)
                .build();
    }

    public BigInteger getTotalSupply(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_jetton_data");

        if (result.getExit_code() != 0) {
            throw new Error("method get_jetton_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber totalSupplyNumber = (TvmStackEntryNumber) result.getStack().get(0);
        return totalSupplyNumber.getNumber();
    }

    /**
     * @param tonlib       Tonlib
     * @param ownerAddress Address
     * @return Address user_jetton_wallet_address
     */
    public Address getJettonWalletAddress(Tonlib tonlib, Address ownerAddress) {
        Address myAddress = this.getAddress();
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeAddress(ownerAddress);

        Deque<String> stack = new ArrayDeque<>();

        stack.offer("[slice, " + cell.endCell().toHex(true) + "]");

        RunResult result = tonlib.runMethod(myAddress, "get_wallet_address", stack);

        if (result.getExit_code() != 0) {
            throw new Error("method get_wallet_address, returned an exit code " + result.getExit_code());
        }

        TvmStackEntrySlice addr = (TvmStackEntrySlice) result.getStack().get(0);
        return NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(addr.getSlice().getBytes())).endCell());
    }

    //    public ExtMessageInfo deploy(Tonlib tonlib, Contract adminWallet, BigInteger walletMsgValue, TweetNaclFast.Signature.KeyPair keyPair) {

    public ExtMessageInfo deploy(Tonlib tonlib, JettonMinterConfig config) {
        long seqno = this.getSeqno(tonlib);
        config.setSeqno(seqno);
        Address ownAddress = getAddress();

//        Cell body = createInternalMessage(ownAddress)
//
//        ExternalMessage extMsg = adminWallet.createTransferMessage(
//                keyPair.getSecretKey(),
//                this.getAddress(),
//                walletMsgValue,
//                seqno,
//                (Cell) null, // body
//                (byte) 3, //send mode
//                this.createStateInit().stateInit);
//
//        return tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc()));


        Cell body = createTransferBody(config);

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .srcAddr(MsgAddressExtNone.builder().build())
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(ownAddress.wc)
                                .address(ownAddress.toBigInteger())
                                .build())
                        .build())
                .init(getStateInit())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(getOptions().getPublicKey(), options.getSecretKey(), body.hash()))
                        .storeRef(body)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }

    public ExtMessageInfo mint(Tonlib tonlib, WalletV3ContractR1 adminWallet, JettonMinterConfig config, TweetNaclFast.Signature.KeyPair keyPair) {

        System.out.println("addr: " + getAddress().toString(true));
        WalletV3Config walletV3Config = WalletV3Config.builder()
                .subWalletId(42)
                .seqno(adminWallet.getSeqno(tonlib))
                .mode(3)
                .validUntil(Instant.now().getEpochSecond() + 5 * 60L)
                .secretKey(keyPair.getSecretKey())
                .publicKey(keyPair.getPublicKey())
                .destination(getAddress())
                .amount(config.getWalletMsgValue())
                .body(createMintBody(0,
                        config.getDestination(),
                        config.getMintMsgValue(),
                        config.getJettonToMintAmount()))
                .build();
        return adminWallet.sendTonCoins(tonlib, walletV3Config);

//        ExternalMessage extMsg = adminWallet.createTransferMessage(
//                keyPair.getSecretKey(),
//                this.getAddress(),
//                walletMsgValue,
//                seqno,
//                );
//
//        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc()));


    }
}