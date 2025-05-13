package org.ton.ton4j.smartcontract;

import static java.util.Objects.isNull;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;

@Builder
@Data
public class LibraryDeployer implements Contract {

  private Tonlib tonlib;
  private long wc;
  Cell libraryDeployerCode;
  Cell libraryCode;

  @Override
  public Tonlib getTonlib() {
    return tonlib;
  }

  @Override
  public void setTonlib(Tonlib pTonlib) {
    tonlib = pTonlib;
  }

  @Override
  public long getWorkchain() {
    return -1;
  }

  @Override
  public String getName() {
    return "LibraryDeployer";
  }

  @Override
  public Cell createCodeCell() {
    if (isNull(libraryDeployerCode)) {
      return CellBuilder.beginCell().fromBoc(WalletCodes.libraryDeployer.getValue()).endCell();
    } else {
      return libraryDeployerCode;
    }
  }

  @Override
  public Cell createDataCell() {
    return libraryCode;
  }

  public static class LibraryDeployerBuilder {}

  public static LibraryDeployerBuilder builder() {
    return new CustomLibraryDeployerBuilder();
  }

  private static class CustomLibraryDeployerBuilder extends LibraryDeployerBuilder {
    @Override
    public LibraryDeployer build() {

      return super.build();
    }
  }

  public Message prepareDeployMsg() {

    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .build();
  }

  public ExtMessageInfo deploy() {
    return tonlib.sendRawMessage(prepareDeployMsg().toCell().toBase64());
  }
}
