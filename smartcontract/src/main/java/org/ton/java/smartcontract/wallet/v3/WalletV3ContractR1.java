package org.ton.java.smartcontract.wallet.v3;

import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Options;

public class WalletV3ContractR1 extends WalletV3ContractBase {
    
    /**
     * @param options Options
     */
    public WalletV3ContractR1(Options options) {
        super(options);
        options.code = Cell.fromBoc(WalletCodes.V3R1.getValue());
        if (options.walletId == null) {
            options.walletId = 698983191 + options.wc;
        }
    }

    @Override
    public String getName() {
        return "v3R1";
    }
}
