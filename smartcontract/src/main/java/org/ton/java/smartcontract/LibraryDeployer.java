package org.ton.java.smartcontract;

import static java.util.Objects.isNull;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.ExternalMessageInInfo;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;

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
            return CellBuilder.beginCell().
                    fromBoc(WalletCodes.libraryDeployer.getValue()).
                    endCell();
        } else {
            return libraryDeployerCode;
        }
    }

    @Override
    public Cell createDataCell() {
        return libraryCode;
    }


    public static class LibraryDeployerBuilder {
    }

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
                .info(ExternalMessageInInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .init(getStateInit())
                .build();
    }

    public ExtMessageInfo deploy() {
        return tonlib.sendRawMessage(prepareDeployMsg().toCell().toBase64());
    }

}
