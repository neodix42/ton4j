package org.ton.java.smartcontract.wallet.v3;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import static java.util.Objects.isNull;

public class WalletV3ContractBase implements Contract<WalletV3Config> {
    Options options;
    Address address;

    protected WalletV3ContractBase(Options options) {
        this.options = options;
    }

    @Override
    public String getName() {
        return "override me";
    }

    @Override
    public Cell createCodeCell() {
        return null;
    }

    /**
     * Creates message payload with subwallet-id, valid-until and seqno, equivalent to:
     * <b subwallet-id 32 u, timestamp 32 i, seqno 32 u, b> // signing message
     */
    public Cell createTransferBody(WalletV3Config config) {

        Cell order = Message.builder()
                .info(InternalMessageInfo.builder()
                        .bounce(config.isBounce())
                        .srcAddr(isNull(config.getSource()) ? null :
                                MsgAddressIntStd.builder()
                                        .workchainId(config.getSource().wc)
                                        .address(config.getSource().toBigInteger())
                                        .build())
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(config.getDestination().wc)
                                .address(config.getDestination().toBigInteger())
                                .build())
                        .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                        .build())
                .init(config.getStateInit())
                .body(config.getBody())
                .build().toCell();

        return CellBuilder.beginCell()
                .storeUint(config.getSubWalletId(), 32)
                .storeUint(config.getValidUntil(), 32)
                .storeUint(config.getSeqno(), 32)
                .storeUint(config.getMode() & 0xff, 8)
                .storeRef(order)
                .endCell();
    }

    @Override
    public Options getOptions() {
        return options;
    }


    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(0, 32); // seqno
        cell.storeUint(getOptions().getWalletId(), 32);
        cell.storeBytes(getOptions().getPublicKey());
        return cell.endCell();
    }

    public Cell createDeployMessage(WalletV3Config config) {
        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(config.getSubWalletId(), 32); //wallet-id

        for (int i = 0; i < 32; i++) { // valid-until
            message.storeBit(true);
        }
        message.storeUint(0, 32); //seqno
        return message.endCell();
    }

    public long getWalletId() {
        return getOptions().walletId;
    }


    public ExtMessageInfo deploy(Tonlib tonlib, WalletV3Config config) {

        Cell body = createDeployMessage(config);

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .init(getStateInit())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(getOptions().getPublicKey(), getOptions().getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }

    public ExtMessageInfo sendTonCoins(Tonlib tonlib, WalletV3Config config) {

        Cell body = createTransferBody(config);

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(config.getPublicKey(), config.getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }
}
