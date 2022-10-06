package org.ton.java.smartcontract.wallet.v2;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;

import java.math.BigInteger;
import java.util.Date;

public class WalletV2ContractBase implements WalletContract {
    Options options;
    Address address;

    protected WalletV2ContractBase(Options options) {
        this.options = options;
    }

    @Override
    public String getName() {
        return "override me";
    }

    /**
     * Creates message payload with seqno and validUntil fields
     *
     * @param seqno long
     * @return Cell
     */
    @Override
    public Cell createSigningMessage(long seqno) {

        CellBuilder message = CellBuilder.beginCell();
        message.storeUint(BigInteger.valueOf(seqno), 32);
        if (seqno == 0) {
            for (int i = 0; i < 32; i++) {
                message.storeBit(true);
            }
        } else {
            Date date = new Date();
            long timestamp = (long) Math.floor(date.getTime() / (double) 1e3);
            message.storeUint(BigInteger.valueOf(timestamp + 60L), 32);
        }
        return message.endCell();
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
