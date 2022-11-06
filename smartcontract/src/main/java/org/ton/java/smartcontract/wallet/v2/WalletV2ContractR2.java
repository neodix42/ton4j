package org.ton.java.smartcontract.wallet.v2;

import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.wallet.Options;

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
}
