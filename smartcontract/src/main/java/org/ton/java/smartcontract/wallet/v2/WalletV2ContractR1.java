package org.ton.java.smartcontract.wallet.v2;


import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Options;

public class WalletV2ContractR1 extends WalletV2ContractBase {

    /**
     * @param options Options
     */
    public WalletV2ContractR1(Options options) {
        super(options);
        options.code = Cell.fromBoc(WalletCodes.V2R1.getValue());
    }

    @Override
    public String getName() {
        return "v2R1";
    }
}
