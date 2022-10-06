package org.ton.java.smartcontract.wallet.v2;


import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.wallet.Options;

public class WalletV2ContractR1 extends WalletV2ContractBase {

    /**
     * @param options Options
     */
    public WalletV2ContractR1(Options options) {
        super(options);
        options.code = Cell.fromBoc("B5EE9C724101010100570000AAFF0020DD2082014C97BA9730ED44D0D70B1FE0A4F2608308D71820D31FD31F01F823BBF263ED44D0D31FD3FFD15131BAF2A103F901541042F910F2A2F800029320D74A96D307D402FB00E8D1A4C8CB1FCBFFC9ED54A1370BB6");
    }

    @Override
    public String getName() {
        return "v2R1";
    }
}
