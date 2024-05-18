package org.ton.java.smartcontract.wallet.v3;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletCodes;

public class WalletV3ContractR1 extends WalletV3ContractBase {
    WalletV3ContractR1(Address address, TweetNaclFast.Signature.KeyPair keyPair, long walletId) {
        super(address, keyPair, walletId);
    }


//    public WalletV3ContractR1(Options options) {
//        super(options);
//        options.code = CellBuilder.beginCell().fromBoc(WalletCodes.V3R1.getValue()).endCell();
//        if (isNull(options.walletId)) {
//            options.walletId = 698983191 + options.wc;
//        }
//    }

    @Override
    public String getName() {
        return "V3R1";
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.V3R1.getValue()).
                endCell();
    }
}
