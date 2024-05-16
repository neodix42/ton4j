package org.ton.java.smartcontract.token.nft;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.ItemData;
import org.ton.java.smartcontract.types.NftItemConfig;
import org.ton.java.smartcontract.types.Royalty;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tlb.types.ExternalMessageInfo;
import org.ton.java.tlb.types.Message;
import org.ton.java.tlb.types.MsgAddressExtNone;
import org.ton.java.tlb.types.MsgAddressIntStd;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.tonlib.types.TvmStackEntrySlice;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class NftItem implements Contract<NftItemConfig> {
    // https://github.com/ton-blockchain/token-contract/blob/1ad314a98d20b41241d5329e1786fc894ad811de/nft/nft-item.fc
    Options options;
    Address address;

    /**
     * @param options Options
     */
    public NftItem(Options options) {
        this.options = options;

        if (nonNull(options.address)) {
            this.address = Address.of(options.address);
        }
        if (isNull(options.wc)) {
            options.wc = nonNull(this.address) ? this.address.wc : 0;
        }

        if (isNull(options.code)) {
            options.code = CellBuilder.beginCell().fromBoc(WalletCodes.nftItem.getValue()).endCell();
        }
    }

    public String getName() {
        return "nftItem";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    /**
     * @return Cell cell contains nft data
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(options.index, 64);
        cell.storeAddress(options.collectionAddress);
        return cell.endCell();
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.nftItem.getValue()).
                endCell();
    }

    @Override
    public Cell createTransferBody(NftItemConfig config) {
        return null;
    }


    /**
     * @return DnsData
     */
    public ItemData getData(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_nft_data");

        if (result.getExit_code() != 0) {
            throw new Error("method get_nft_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber isInitializedNumber = (TvmStackEntryNumber) result.getStack().get(0);
        boolean isInitialized = isInitializedNumber.getNumber().longValue() == -1;

        BigInteger index = ((TvmStackEntryNumber) result.getStack().get(1)).getNumber();

        TvmStackEntrySlice collectionAddr = (TvmStackEntrySlice) result.getStack().get(2);
        Address collectionAddress = NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(collectionAddr.getSlice().getBytes()).endCell());

        TvmStackEntrySlice ownerAddr = (TvmStackEntrySlice) result.getStack().get(3);
        Address ownerAddress = isInitialized ? NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(ownerAddr.getSlice().getBytes()).endCell()) : null;

        TvmStackEntrySlice contentCell = (TvmStackEntrySlice) result.getStack().get(4);
        Cell cell = CellBuilder.beginCell().fromBoc(contentCell.getSlice().getBytes()).endCell();

        String contentUri = null;
        try {
            if (isInitialized && nonNull(collectionAddress)) {
                contentUri = NftUtils.parseOffchainUriCell(cell);
            }
        } catch (Error e) {
            //todo
        }
        return ItemData.builder()
                .isInitialized(isInitialized)
                .index(index)
                .collectionAddress(collectionAddress)
                .ownerAddress(ownerAddress)
                .contentCell(cell)
                .contentUri(contentUri)
                .build();
    }


    /**
     * @param queryId         long optional, default 0
     * @param newOwnerAddress Address
     * @param forwardAmount   BigInteger optional, default 0
     * @param forwardPayload  byte[] optional, default null
     * @param responseAddress Address
     */
    public static Cell createTransferBody(long queryId, Address newOwnerAddress, BigInteger forwardAmount, byte[] forwardPayload, Address responseAddress) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(0x5fcc3d14, 32); // transfer op
        cell.storeUint(queryId, 64);
        cell.storeAddress(newOwnerAddress);
        cell.storeAddress(responseAddress);
        cell.storeBit(false); // null custom_payload
        cell.storeCoins(forwardAmount); //
        cell.storeBit(false); // forward_payload in this slice, not separate cell

        if (nonNull(forwardPayload)) {
            cell.storeBytes(forwardPayload);
        }
        return cell.endCell();
    }


    public ExtMessageInfo deploy(Tonlib tonlib, NftItemConfig config) {

//        long seqno = wallet.getSeqno(tonlib);
//
//        ExternalMessage extMsg = wallet.createTransferMessage(
//                keyPair.getSecretKey(),
//                nftCollectionAddress,
//                msgValue,
//                seqno,
//                NftCollection.createMintBody(
//                        0,
//                        index,
//                        msgValue,
//                        wallet.getAddress(),
//                        nftItemContentUri));
//
//        return tonlib.sendRawMessage(extMsg.message.toBase64());

        // should be internal msg
        Address ownAddress = getAddress();

        Cell body = NftCollection.createMintBody(
                0,
                config.getIndex(),
                config.getAmount(),
                this.getAddress(),
                config.getNftItemContentUri());

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

    /**
     * @param queryId long, default 0
     * @return Cell
     */
    public static Cell createGetStaticDataBody(long queryId) {
        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(0x2fcb26a2, 32); // op::get_static_data() asm "0x2fcb26a2 PUSHINT";
        body.storeUint(queryId, 64); // query_id
        return body.endCell();
    }


    /**
     * for single nft without collection
     *
     * @return Roaylty
     */
    public Royalty getRoyaltyParams(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        return NftUtils.getRoyaltyParams(tonlib, myAddress);
    }
}
