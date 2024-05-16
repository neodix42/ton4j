package org.ton.java.smartcontract.wallet.v2;


import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Options;

public class WalletV2ContractR1 extends WalletV2ContractBase {

    /**
     * @param options Options
     */
    public WalletV2ContractR1(Options options) {
        super(options);
        options.code = CellBuilder.beginCell().fromBoc(WalletCodes.V2R1.getValue()).endCell();
    }

    @Override
    public String getName() {
        return "V2R1";
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.V2R1.getValue()).
                endCell();
    }
}
