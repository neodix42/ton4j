package org.ton.java.smartcontract.token.nft;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.NftMarketPlaceConfig;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tlb.types.ExternalMessageInfo;
import org.ton.java.tlb.types.Message;
import org.ton.java.tlb.types.MsgAddressExtNone;
import org.ton.java.tlb.types.MsgAddressIntStd;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class NftMarketplace implements Contract<NftMarketPlaceConfig> {
    public static final String NFT_MARKETPLACE_CODE_HEX = "B5EE9C7241010401006D000114FF00F4A413F4BCF2C80B01020120020300AAD23221C700915BE0D0D3030171B0915BE0FA40ED44D0FA403012C705F2E19101D31F01C0018E2BFA003001D4D43021F90070C8CA07CBFFC9D077748018C8CB05CB0258CF165004FA0213CB6BCCCCC971FB00915BE20004F2308EF7CCE7";

    Options options;
    Address address;

    /**
     * @param options Options, required adminAddress (owner)
     */
    public NftMarketplace(Options options) {
        this.options = options;
        this.options.wc = 0;

        if (nonNull(options.address)) {
            this.address = Address.of(options.address);
        }

        if (isNull(options.code)) {
            options.code = CellBuilder.beginCell().fromBoc(NFT_MARKETPLACE_CODE_HEX).endCell();
        }
    }

    public String getName() {
        return "nftMarketplace";
    }

    @Override
    public Options getOptions() {
        return options;
    }


    /**
     * @return Cell cell contains nft marketplace data
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeAddress(options.adminAddress);
        return cell.endCell();
    }

    @Override
    public Cell createTransferBody(NftMarketPlaceConfig config) {
        return null;
    }

    public ExtMessageInfo deploy(Tonlib tonlib, NftMarketPlaceConfig config) {

        long seqno = this.getSeqno(tonlib);
        config.setSeqno(seqno);
        Address ownAddress = getAddress();

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .srcAddr(MsgAddressExtNone.builder().build())
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(ownAddress.wc)
                                .address(ownAddress.toBigInteger())
                                .build())
                        .build())
                .init(createStateInit())
//                .body(CellBuilder.beginCell()
//                        .storeBytes(Utils.signData(getOptions().getPublicKey(), options.getSecretKey(), body.hash()))
//                        .storeRef(body)
//                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }
}
