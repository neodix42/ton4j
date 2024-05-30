package org.ton.java.smartcontract.wallet.v1;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV1R1Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;

@Builder
@Getter
public class WalletV1R1 implements Contract {

    TweetNaclFast.Signature.KeyPair keyPair;
    long initialSeqno;

    public static class WalletV1R1Builder {
    }

    public static WalletV1R1Builder builder() {
        return new CustomWalletV1R1Builder();
    }

    private static class CustomWalletV1R1Builder extends WalletV1R1Builder {
        @Override
        public WalletV1R1 build() {
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
        return "V1R1";
    }

    @Override
    public Cell createDataCell() {
        return CellBuilder.beginCell()
                .storeUint(initialSeqno, 32) // seqno
                .storeBytes(keyPair.getPublicKey())
                .endCell();
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.V1R1.getValue()).
                endCell();
    }

    Cell createDeployMessage() {
        return CellBuilder.beginCell().storeUint(initialSeqno, 32).endCell();
    }

    Cell createTransferBody(WalletV1R1Config config) {
        Cell order = Message.builder()
                .info(InternalMessageInfo.builder()
                        .bounce(config.getBounce())
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(config.getDestination().wc)
                                .address(config.getDestination().toBigInteger())
                                .build())
                        .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                        .build())
                .init(config.getStateInit())
                .body(CellBuilder.beginCell()
                        .storeUint(0, 32)
                        .storeString(config.getComment())
                        .endCell())
                .build().toCell();

        return CellBuilder.beginCell()
                .storeUint(BigInteger.valueOf(config.getSeqno()), 32)
                .storeUint((config.getMode() == 0) ? 3 : config.getMode() & 0xff, 8)
                .storeRef(order)
                .endCell();
    }


    /**
     * Sends amount of nano toncoins to destination address using specified seqno
     *
     * @param config WalletV1R1Config
     */
    public ExtMessageInfo send(WalletV1R1Config config) {
        return tonlib.sendRawMessage(prepareExternalMsg(config).toCell().toBase64());
    }

    public Message prepareExternalMsg(WalletV1R1Config config) {
        Cell body = createTransferBody(config);
        return MsgUtils.createExternalMessageWithSignedBody(keyPair, getAddress(), null, body);
    }

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
}
