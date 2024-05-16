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
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;

@Builder
@Getter
public class WalletV1ContractR1 implements Contract<WalletV1R1Config> {

    Options options;
    TweetNaclFast.Signature.KeyPair keyPair;
    int wc;
    Cell code; // for PoC
//    Address address;
//    StateInit stateInit;

    public static class WalletV1ContractR1Builder {
        WalletV1ContractR1Builder() {
            if (isNull(keyPair)) {
                keyPair = Utils.generateSignatureKeyPair();
            }
        }
    }


//    public WalletV1ContractR1(Options options) {
//        this.options = options;
//        options.code = CellBuilder.beginCell().fromBoc(WalletCodes.V1R1.getValue()).endCell();
//    }

    @Override
    public Options getOptions() {
        return null;
    }

    @Override
    public String getName() {
        return "V1R1";
    }

//    @Override
//    public Address getAddress() {
//        if (isNull(address)) {
//            address = getStateInit().getAddress();
//            return address;
//        } else {
//            return address;
//        }
//    }
//
//    @Override
//    public StateInit getStateInit() {
//        if (isNull(stateInit)) {
//            stateInit = StateInit.builder()
//                    .code(createCodeCell())
//                    .data(createDataCell())
//                    .build();
//            return stateInit;
//        } else {
//            return stateInit;
//        }
//    }

    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(BigInteger.ZERO, 32); // seqno
        cell.storeBytes(keyPair.getPublicKey());
        return cell.endCell();
    }

    @Override
    public Cell createCodeCell() {
        if (isNull(code)) {
            return CellBuilder.beginCell().
                    fromBoc(WalletCodes.V1R1.getValue()).
                    endCell();
        } else {
            return code;
        }
    }

    public Cell createDeployMessage() {
        return CellBuilder.beginCell().storeUint(BigInteger.ZERO, 32).endCell();
    }

    public Cell createTransferBody(WalletV1R1Config config) {
        Cell body = CellBuilder.beginCell()
                .storeUint(0, 32)
                .storeString(config.getComment())
                .endCell();

        Cell order = MsgUtils.createInternalMessage(config.getDestination(), config.getAmount(), null, body).toCell();

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
     * @param config WalletV1R1Config
     */
    public ExtMessageInfo send(Tonlib tonlib, WalletV1R1Config config) {
        Cell body = isNull(config.getBody()) ? createTransferBody(config) : config.getBody();
        Message externalMessage = MsgUtils.createExternalMessageWithSignedBody(keyPair, getAddress(), getStateInit(), body);

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }


    public ExtMessageInfo deploy(Tonlib tonlib, WalletV1R1Config config) {
        Cell body = isNull(config) ? createDeployMessage() : config.getBody();

        Message externalMessage = MsgUtils.createExternalMessageWithSignedBody(keyPair, getAddress(), getStateInit(), body);
//        Message externalMessage = Message.builder()
//                .info(ExternalMessageInfo.builder()
//                        .dstAddr(getAddressIntStd())
//                        .build())
//                .init(this.getStateInit())
//                .body(CellBuilder.beginCell()
//                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
//                        .storeCell(body)
//                        .endCell())
//                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }
}
