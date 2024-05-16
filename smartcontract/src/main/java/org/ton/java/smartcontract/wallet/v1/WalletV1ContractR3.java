package org.ton.java.smartcontract.wallet.v1;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV1R3Config;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;

@Builder
@Getter
public class WalletV1ContractR3 implements Contract<WalletV1R3Config> {

    Options options;
    TweetNaclFast.Signature.KeyPair keyPair;
    int wc;
    Cell code;


    public static class WalletV1ContractR3Builder {
        WalletV1ContractR3Builder() {
            if (isNull(keyPair)) {
                keyPair = Utils.generateSignatureKeyPair();
            }
        }
    }

//    public WalletV1ContractR3(Options options) {
//        this.options = options;
//        options.code = CellBuilder.beginCell().fromBoc(WalletCodes.V1R3.getValue()).endCell();
//    }

    @Override
    public String getName() {
        return "V1R3";
    }

    @Override
    public Options getOptions() {
        return options;
    }


    public String getPublicKey(Tonlib tonlib) {

        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_public_key");

        if (result.getExit_code() != 0) {
            throw new Error("method get_public_key, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber publicKeyNumber = (TvmStackEntryNumber) result.getStack().get(0);
        return publicKeyNumber.getNumber().toString(16);
    }

    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(0, 32); // seqno
        cell.storeBytes(keyPair.getPublicKey());
        return cell.endCell();
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.V1R3.getValue()).
                endCell();
    }

    public Cell createDeployMessage() {
        return CellBuilder.beginCell().storeUint(BigInteger.ZERO, 32).endCell();
    }

    @Override
    public Cell createTransferBody(WalletV1R3Config config) {
        Cell order = Message.builder()
                .info(InternalMessageInfo.builder()
                        .bounce(config.isBounce())
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(config.getDestination().wc)
                                .address(config.getDestination().toBigInteger())
                                .build())
                        .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                        .build())
                .body(CellBuilder.beginCell()
                        .storeUint(0, 32)
                        .storeString(config.getComment())
                        .endCell())
                .build().toCell();

        return CellBuilder.beginCell()
                .storeUint(BigInteger.valueOf(config.getSeqno()), 32)
                .storeUint(config.getMode() & 0xff, 8)
                .storeRef(order)
                .endCell();
    }

    /**
     * Sends amount of nano toncoins to destination address using specified seqno
     *
     * @param tonlib Tonlib
     * @param config WalletV1R2Config
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, WalletV1R3Config config) {
//        long seqno = getSeqno(tonlib);
//        config.setSeqno(seqno);

        Cell body = createTransferBody(config);

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }

    public ExtMessageInfo deploy(Tonlib tonlib, WalletV1R3Config config) {

        Cell body = createDeployMessage();

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .init(getStateInit())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }
}
