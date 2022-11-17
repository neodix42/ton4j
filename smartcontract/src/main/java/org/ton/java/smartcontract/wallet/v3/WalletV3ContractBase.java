package org.ton.java.smartcontract.wallet.v3;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;

import java.math.BigInteger;
import java.util.Date;

public class WalletV3ContractBase implements WalletContract {
    Options options;
    Address address;

    protected WalletV3ContractBase(Options options) {
        this.options = options;
    }

    @Override
    public String getName() {
        return "override me";
    }

    /**
     * Creates messsage payload with subwallet-id, valid-until and seqno, equivalent to:
     * <b subwallet-id 32 u, timestamp 32 i, seqno 32 u, b> // signing message
     *
     * @param seqno long
     * @return Cell
     */
    @Override
    public Cell createSigningMessage(long seqno) {

        CellBuilder message = CellBuilder.beginCell();

        message.storeUint(BigInteger.valueOf(getOptions().walletId), 32);

        if (seqno == 0) {
            for (int i = 0; i < 32; i++) {
                message.storeBit(true);
            }
        } else {
            Date date = new Date();
            long timestamp = (long) Math.floor(date.getTime() / 1e3);
            message.storeUint(BigInteger.valueOf(timestamp + 60L), 32);
        }

        message.storeUint(BigInteger.valueOf(seqno), 32);
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

    /**
     * <b 0 32 u, subwallet-id 32 u, file-base +".pk" load-generate-keypair B, b> // data cell
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(BigInteger.ZERO, 32);
        cell.storeUint(BigInteger.valueOf(getOptions().walletId), 32);
        cell.storeBytes(getOptions().publicKey);
        return cell.endCell();
    }

    public long getWalletId() {
        return getOptions().walletId;
    }

    public void deploy(Tonlib tonlib, byte[] secretKey) {
        tonlib.sendRawMessage(createInitExternalMessage(secretKey).message.toBocBase64(false));
    }

    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno);
        tonlib.sendRawMessage(msg.message.toBocBase64(false));
    }
}
