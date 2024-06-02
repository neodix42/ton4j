package org.ton.java.smartcontract;


import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.ExternalMessageInfo;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import static java.util.Objects.isNull;

@Builder
@Getter
public class GenericSmartContract implements Contract {

    TweetNaclFast.Signature.KeyPair keyPair;
    String code;
    String data;
    private Tonlib tonlib;
    private long wc;


    @Override
    public long getWorkchain() {
        return wc;
    }

    @Override
    public String getName() {
        return "GenericSmartContract";
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell()
                .fromBoc(code)
                .endCell();
    }

    @Override
    public Cell createDataCell() {
        return CellBuilder.beginCell()
                .fromBoc(data)
                .endCell();
    }


    public static class GenericSmartContractBuilder {
    }

    public static GenericSmartContractBuilder builder() {
        return new CustomGenericSmartContractBuilder();
    }

    private static class CustomGenericSmartContractBuilder extends GenericSmartContractBuilder {
        @Override
        public GenericSmartContract build() {
            if (isNull(super.keyPair)) {
                super.keyPair = Utils.generateSignatureKeyPair();
            }
            return super.build();
        }
    }

    public ExtMessageInfo deploy(Cell deployMessageBody) {
        return tonlib.sendRawMessage(prepareDeployMsg(deployMessageBody).toCell().toBase64());
    }

    public Message prepareDeployMsg(Cell deployMessageBody) {

        Cell body = deployMessageBody;

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