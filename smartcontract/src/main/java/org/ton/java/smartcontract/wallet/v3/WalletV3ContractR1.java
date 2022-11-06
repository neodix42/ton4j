package org.ton.java.smartcontract.wallet.v3;

import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.wallet.Options;

public class WalletV3ContractR1 extends WalletV3ContractBase {

    public static final String V3_R1_CODE_HEX = "B5EE9C724101010100620000C0FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED543FBE6EE0";

    /**
     * @param options Options
     */
    public WalletV3ContractR1(Options options) {
        super(options);
        options.code = Cell.fromBoc(V3_R1_CODE_HEX);
        if (options.walletId == null) {
            options.walletId = 698983191 + options.wc;
        }
    }

    @Override
    public String getName() {
        return "v3R1";
    }
}
