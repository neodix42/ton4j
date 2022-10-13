package org.ton.java.smartcontract.lockup;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonPfxHashMapE;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.List;

/**
 * https://github.com/toncenter/tonweb/tree/master/src/contract/lockup
 * Funding the wallet with custom time-locks is out of scope for this implementation at the time. This can be performed by specialized software.
 */
public class LockupWalletV1 implements WalletContract {

    Options options;
    Address address;

    /**
     * Additional options should be populated.
     * options.config.configPublicKey
     * options.config.allowedDestinations
     *
     * @param options Options
     */
    public LockupWalletV1(Options options) {
        this.options = options;
        options.code = Cell.fromBoc("B5EE9C7241021E01000261000114FF00F4A413F4BCF2C80B010201200203020148040501F2F28308D71820D31FD31FD31F802403F823BB13F2F2F003802251A9BA1AF2F4802351B7BA1BF2F4801F0BF9015410C5F9101AF2F4F8005057F823F0065098F823F0062071289320D74A8E8BD30731D4511BDB3C12B001E8309229A0DF72FB02069320D74A96D307D402FB00E8D103A4476814154330F004ED541D0202CD0607020120131402012008090201200F100201200A0B002D5ED44D0D31FD31FD3FFD3FFF404FA00F404FA00F404D1803F7007434C0C05C6C2497C0F83E900C0871C02497C0F80074C7C87040A497C1383C00D46D3C00608420BABE7114AC2F6C2497C338200A208420BABE7106EE86BCBD20084AE0840EE6B2802FBCBD01E0C235C62008087E4055040DBE4404BCBD34C7E00A60840DCEAA7D04EE84BCBD34C034C7CC0078C3C412040DD78CA00C0D0E00130875D27D2A1BE95B0C60000C1039480AF00500161037410AF0050810575056001010244300F004ED540201201112004548E1E228020F4966FA520933023BB9131E2209835FA00D113A14013926C21E2B3E6308003502323287C5F287C572FFC4F2FFFD00007E80BD00007E80BD00326000431448A814C4E0083D039BE865BE803444E800A44C38B21400FE809004E0083D10C06002012015160015BDE9F780188242F847800C02012017180201481B1C002DB5187E006D88868A82609E00C6207E00C63F04EDE20B30020158191A0017ADCE76A268699F98EB85FFC00017AC78F6A268698F98EB858FC00011B325FB513435C2C7E00017B1D1BE08E0804230FB50F620002801D0D3030178B0925B7FE0FA4031FA403001F001A80EDAA4");
        if (options.walletId == null) {
            options.walletId = 698983191 + options.wc;
        }
    }

    @Override
    public String getName() {
        return "lockupR1";
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

    /**
     * from restricted.fc:
     * store_int(seqno, 32)
     * store_int(subwallet_id, 32)
     * store_uint(public_key, 256)
     * store_uint(config_public_key, 256)
     * store_dict(allowed_destinations)
     * store_grams(total_locked_value)
     * store_dict(locked)
     * store_grams(total_restricted_value)
     * store_dict(restricted).end_cell();
     *
     * @return Cell
     */
    @Override
    public Cell createDataCell() {

        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(BigInteger.ZERO, 32); // seqno
        cell.storeUint(BigInteger.valueOf(getOptions().walletId), 32);
        cell.storeBytes(getOptions().publicKey); //256
        cell.storeBytes(Utils.hexToBytes(options.lockupConfig.configPublicKey)); // 256

        int dictKeySize = 267;
        TonPfxHashMapE dictAllowedDestinations = new TonPfxHashMapE(dictKeySize);

        if ((options.lockupConfig.allowedDestinations != null) && (!options.lockupConfig.allowedDestinations.isEmpty())) {
            for (String addr : options.lockupConfig.allowedDestinations) {
                dictAllowedDestinations.elements.put(Address.of(addr), (byte) 1);
            }
        }

        Cell cellDict = dictAllowedDestinations.serialize(
                k -> CellBuilder.beginCell().storeAddress((Address) k).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 8)
        );
        cell.storeDict(cellDict);

        cell.storeCoins(BigInteger.ZERO);   // .store_grams(total_locked_value)
        cell.storeBit(false);               // empty locked dict
        cell.storeCoins(BigInteger.ZERO);   // .store_grams(total_restricted_value)
        cell.storeBit(false);               // empty locked dict

        return cell.endCell();
    }

    /**
     * @return long
     */
    public long getWalletId(Tonlib tonlib) {

        Address myAddress = getAddress();
        RunResult result = tonlib.runMethod(myAddress, "wallet_id");
        TvmStackEntryNumber subWalletId = (TvmStackEntryNumber) result.getStackEntry().get(0);

        return subWalletId.getNumber().longValue();
    }

    public String getPublicKey(Tonlib tonlib) {

        Address myAddress = getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_public_key");
        TvmStackEntryNumber pubkey = (TvmStackEntryNumber) result.getStackEntry().get(0);

        return pubkey.getNumber().toString(16);
    }

    public boolean check_destination(Tonlib tonlib, String destination) {

        Address myAddress = getAddress();

        Deque<String> stack = new ArrayDeque<>();

        CellBuilder c = CellBuilder.beginCell();
        c.storeAddress(Address.of(destination));
        stack.offer("[slice, " + c.endCell().toHex(true) + "]");

        RunResult result = tonlib.runMethod(myAddress, "check_destination", stack);
        TvmStackEntryNumber found = (TvmStackEntryNumber) result.getStackEntry().get(0);

        return (found.getNumber().intValue() == -1);
    }

    /**
     * @return BigInteger Amount of nano-coins that can be spent immediately.
     */
    public BigInteger getLiquidBalance(Tonlib tonlib) {
        List<BigInteger> balances = getBalances(tonlib);
        return balances.get(0).subtract(balances.get(1)).subtract(balances.get(2));
    }

    /**
     * @return BigInteger Amount of nano-coins that can be spent after the time-lock OR to the whitelisted addresses.
     */
    public BigInteger getNominalRestrictedBalance(Tonlib tonlib) {
        return getBalances(tonlib).get(1);
    }

    /**
     * @return BigInteger Amount of nano-coins that can be spent after the time-lock only (whitelisted addresses not used).
     */
    public BigInteger getNominalLockedBalance(Tonlib tonlib) {
        return getBalances(tonlib).get(2);
    }

    /**
     * @return BigInteger Total amount of nano-coins on the contract
     * nominal liquid value
     * nominal restricted value
     * nominal locked value
     */
    public List<BigInteger> getBalances(Tonlib tonlib) {
        Address myAddress = getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_balances");
        TvmStackEntryNumber balance = (TvmStackEntryNumber) result.getStackEntry().get(0); // ton balance
        TvmStackEntryNumber restrictedValue = (TvmStackEntryNumber) result.getStackEntry().get(1); // total_restricted_value
        TvmStackEntryNumber lockedValue = (TvmStackEntryNumber) result.getStackEntry().get(2); // total_locked_value

        return List.of(
                balance.getNumber(),
                restrictedValue.getNumber(),
                lockedValue.getNumber()
        );
    }

    /**
     * Get current seqno
     *
     * @return long
     */
    public long getSeqno(Tonlib tonlib) {

        Address myAddress = getAddress();
        RunResult result = tonlib.runMethod(myAddress, "seqno");
        TvmStackEntryNumber seqno = (TvmStackEntryNumber) result.getStackEntry().get(0);

        return seqno.getNumber().longValue();
    }

    public boolean sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount) {
        try {
            long seqno = getSeqno(tonlib);
            //createTransferMessage with payload - no , see createInternalMessageHeader contains src and dest
            ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno);
            ExtMessageInfo result = tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));
            System.out.println(result);
            return true;
        } catch (Throwable e) {
            System.err.println("Error sending TonCoins to " + destinationAddress.toString() + ". " + e.getMessage());
            return false;
        }
    }
}
