package org.ton.java.smartcontract.wallet.v2;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;

public class WalletV2ContractR2 extends WalletV2ContractBase {

    public static final String V2_R2_CODE_HEX = "B5EE9C724101010100630000C2FF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F2608308D71820D31FD31F01F823BBF263ED44D0D31FD3FFD15131BAF2A103F901541042F910F2A2F800029320D74A96D307D402FB00E8D1A4C8CB1FCBFFC9ED54044CD7A1";

    /**
     * @param options Options
     */
    public WalletV2ContractR2(Options options) {
        super(options);
        options.code = Cell.fromBoc(V2_R2_CODE_HEX);
    }

    @Override
    public String getName() {
        return "v2R2";
    }

    public String getPublicKey(Tonlib tonlib) {

        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_public_key");

        if (result.getExit_code() != 0) {
            throw new Error("method get_public_key, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber publicKeyNumber = (TvmStackEntryNumber) result.getStackEntry().get(0);
        return publicKeyNumber.getNumber().toString(16);
    }
}
