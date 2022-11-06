package org.ton.java.smartcontract.wallet.v1;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;

public class SimpleWalletContractR2 implements WalletContract {

    public static final String V1_R2_CODE_HEX = "B5EE9C724101010100530000A2FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED54D0E2786F";
    Options options;
    Address address;

    /**
     * @param options Options
     */
    public SimpleWalletContractR2(Options options) {
        this.options = options;
        options.code = Cell.fromBoc(V1_R2_CODE_HEX);
    }

    @Override
    public String getName() {
        return "simpleR2";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public Address getAddress() {
        if (address == null) {
            return (createStateInit()).address;
        }
        return address;
    }
}
