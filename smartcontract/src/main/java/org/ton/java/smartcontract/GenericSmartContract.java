package org.ton.java.smartcontract;

import static java.util.Objects.isNull;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.ExternalMessageInfo;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

@Builder
@Data
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
    return CellBuilder.beginCell().fromBoc(code).endCell();
  }

  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell().fromBoc(data).endCell();
  }

  public static class GenericSmartContractBuilder {}

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

  /**
   * Deploy with body
   *
   * @param deployMessageBody usually stands for internal message
   * @return ExtMessageInfo
   */
  public ExtMessageInfo deploy(Cell deployMessageBody) {
    return tonlib.sendRawMessage(prepareDeployMsg(deployMessageBody).toCell().toBase64());
  }

  /**
   * Deploy without body
   *
   * @return ExtMessageInfo
   */
  public ExtMessageInfo deploy() {
    return tonlib.sendRawMessage(prepareDeployMsgWithoutBody().toCell().toBase64());
  }

  public Message prepareDeployMsgWithoutBody() {
    return Message.builder()
        .info(ExternalMessageInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .build();
  }

  public Message prepareDeployMsg(Cell deployMessageBody) {

    return Message.builder()
        .info(ExternalMessageInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(
            CellBuilder.beginCell()
                .storeBytes(
                    Utils.signData(
                        keyPair.getPublicKey(), keyPair.getSecretKey(), deployMessageBody.hash()))
                .storeCell(deployMessageBody)
                .endCell())
        .build();
  }
}
