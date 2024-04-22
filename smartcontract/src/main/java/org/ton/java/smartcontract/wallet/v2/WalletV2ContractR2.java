package org.ton.java.smartcontract.wallet.v2;

import org.ton.java.address.Address;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;

public class WalletV2ContractR2 extends WalletV2ContractBase {

    /**
     * @param options Options
     */
    public WalletV2ContractR2(Options options) {
        super(options);
        options.code = CellBuilder.beginCell().fromBoc(WalletCodes.V2R2.getValue()).endCell();
    }

    @Override
    public String getName() {
        return "V2R2";
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
}
