package org.ton.java.smartcontract.wallet.v4;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.DeployedPlugin;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.NewPlugin;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.*;

public class WalletV4ContractR2 implements WalletContract {

    Options options;
    Address address;

    /**
     * @param options Options
     */
    public WalletV4ContractR2(Options options) {
        this.options = options;
        options.code = Cell.fromBoc("B5EE9C72410214010002D4000114FF00F4A413F4BCF2C80B010201200203020148040504F8F28308D71820D31FD31FD31F02F823BBF264ED44D0D31FD31FD3FFF404D15143BAF2A15151BAF2A205F901541064F910F2A3F80024A4C8CB1F5240CB1F5230CBFF5210F400C9ED54F80F01D30721C0009F6C519320D74A96D307D402FB00E830E021C001E30021C002E30001C0039130E30D03A4C8CB1F12CB1FCBFF1011121302E6D001D0D3032171B0925F04E022D749C120925F04E002D31F218210706C7567BD22821064737472BDB0925F05E003FA403020FA4401C8CA07CBFFC9D0ED44D0810140D721F404305C810108F40A6FA131B3925F07E005D33FC8258210706C7567BA923830E30D03821064737472BA925F06E30D06070201200809007801FA00F40430F8276F2230500AA121BEF2E0508210706C7567831EB17080185004CB0526CF1658FA0219F400CB6917CB1F5260CB3F20C98040FB0006008A5004810108F45930ED44D0810140D720C801CF16F400C9ED540172B08E23821064737472831EB17080185005CB055003CF1623FA0213CB6ACB1FCB3FC98040FB00925F03E20201200A0B0059BD242B6F6A2684080A06B90FA0218470D4080847A4937D29910CE6903E9FF9837812801B7810148987159F31840201580C0D0011B8C97ED44D0D70B1F8003DB29DFB513420405035C87D010C00B23281F2FFF274006040423D029BE84C600201200E0F0019ADCE76A26840206B90EB85FFC00019AF1DF6A26840106B90EB858FC0006ED207FA00D4D422F90005C8CA0715CBFFC9D077748018C8CB05CB0222CF165005FA0214CB6B12CCCCC973FB00C84014810108F451F2A7020070810108D718FA00D33FC8542047810108F451F2A782106E6F746570748018C8CB05CB025006CF165004FA0214CB6A12CB1FCB3FC973FB0002006C810108D718FA00D33F305224810108F459F2A782106473747270748018C8CB05CB025005CF165003FA0213CB6ACB1F12CB3FC973FB00000AF400C9ED54696225E5");
        if (options.walletId == null) {
            options.walletId = 698983191 + options.wc;
        }
    }

    @Override
    public String getName() {
        return "v4R2";
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

    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(BigInteger.ZERO, 32);
        cell.storeUint(BigInteger.valueOf(getOptions().walletId), 32);
        cell.storeBytes(getOptions().publicKey);
        cell.storeUint(BigInteger.ZERO, 1); //plugins dict empty
        return cell.endCell();
    }

    @Override
    public Cell createSigningMessage(long seqno) {
        return createSigningMessage(seqno, false);
    }

    /**
     * @param seqno     long
     * @param withoutOp boolean
     * @return Cell
     */

    public Cell createSigningMessage(long seqno, boolean withoutOp) {

        CellBuilder message = CellBuilder.beginCell();

        message.storeUint(BigInteger.valueOf(getOptions().walletId), 32);

        if (seqno == 0) {
            for (int i = 0; i < 32; i++) {
                message.storeBit(true);
            }
        } else {
            Date date = new Date();
            long timestamp = (long) Math.floor(date.getTime() / (double) 1e3);
            message.storeUint(BigInteger.valueOf(timestamp + 60L), 32);
        }

        message.storeUint(BigInteger.valueOf(seqno), 32);

        if (!withoutOp) {
            message.storeUint(BigInteger.ZERO, 8); // op
        }

        return message.endCell();
    }

    /**
     * @param params NewPlugin
     */
    public ExternalMessage deployAndInstallPlugin(NewPlugin params) {

        Cell signingMessage = createSigningMessage(params.seqno, true);
        signingMessage.bits.writeUint(BigInteger.ONE, 8); // op
        signingMessage.bits.writeInt(BigInteger.valueOf(params.pluginWc), 8);
        signingMessage.bits.writeCoins(params.amount); // plugin balance
        signingMessage.refs.add(params.stateInit);
        signingMessage.refs.add(params.body);
        return createExternalMessage(signingMessage, params.secretKey, params.seqno, false);
    }

    /**
     * @param params    DeployedPlugin,
     * @param isInstall boolean install or uninstall
     */
    ExternalMessage setPlugin(DeployedPlugin params, boolean isInstall) {

        Address pluginAddress = new Address(params.pluginAddress);

        Cell signingMessage = createSigningMessage(params.seqno, true);
        signingMessage.bits.writeUint(isInstall ? BigInteger.TWO : BigInteger.valueOf(3), 8); // op
        signingMessage.bits.writeInt(BigInteger.valueOf(params.pluginAddress.wc), 8);
        signingMessage.bits.writeBytes(pluginAddress.hashPart);
        signingMessage.bits.writeCoins(BigInteger.valueOf(params.amount.longValue()));
        signingMessage.bits.writeUint(BigInteger.valueOf(params.queryId), 64);

        return this.createExternalMessage(signingMessage, params.secretKey, params.seqno, false);
    }

    /**
     * Installs/assigns plugin into wallet-v4
     *
     * @param params DeployedPlugin
     */
    public ExternalMessage installPlugin(DeployedPlugin params) {
        return setPlugin(params, true);
    }

    /**
     * Uninstalls/removes plugin from wallet-v4
     *
     * @param params DeployedPlugin
     */
    public ExternalMessage removePlugin(DeployedPlugin params) {
        return setPlugin(params, false);
    }

    /**
     * Get current seqno of wallet-v4
     *
     * @return long
     */
    public long getSeqno(Tonlib tonlib) {

        Address myAddress = getAddress();
        RunResult result = tonlib.runMethod(myAddress, "seqno");
        TvmStackEntryNumber seqno = (TvmStackEntryNumber) result.getStackEntry().get(0);

        return seqno.getNumber().longValue();
    }

    /**
     * @return long subwallet-id
     */
    public long getWalletId(Tonlib tonlib) {

        Address myAddress = getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_subwallet_id");
        TvmStackEntryNumber subWalletId = (TvmStackEntryNumber) result.getStackEntry().get(0);

        return subWalletId.getNumber().longValue();
    }

    public byte[] getPublicKey(Tonlib tonlib) {
        Address myAddress = getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_public_key");
        TvmStackEntryNumber pubKey = (TvmStackEntryNumber) result.getStackEntry().get(0);

        return pubKey.getNumber().toByteArray();
    }

    /**
     * @param pluginAddress Address
     * @return boolean
     */
    public boolean isPluginInstalled(Tonlib tonlib, Address pluginAddress) {
        pluginAddress = new Address(pluginAddress);
        String hashPart = "0x" + Utils.bytesToHex(pluginAddress.hashPart);

        Address myAddress = getAddress();

        Deque<String> stack = new ArrayDeque<>();
        stack.offer("[num, " + pluginAddress.wc + "]");
        stack.offer("[num, " + hashPart + "]");

        RunResult result = tonlib.runMethod(myAddress, "is_plugin_installed", stack);
        TvmStackEntryNumber resultNumber = (TvmStackEntryNumber) result.getStackEntry().get(0);

        return resultNumber.getNumber().longValue() != 0;
    }

    /**
     * @return List<String> plugins addresses
     */
    public List<String> getPluginsList(Tonlib tonlib) {
        List<String> r = new ArrayList<>();
        Address myAddress = getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_plugin_list");
        TvmStackEntryList list = (TvmStackEntryList) result.getStackEntry().get(0);
        for (Object o : list.getList().getElements()) {
            TvmStackEntryTuple t = (TvmStackEntryTuple) o;
            TvmTuple tuple = t.getTuple();
            TvmStackEntryNumber wc = (TvmStackEntryNumber) tuple.getElements().get(0); // 1 byte
            TvmStackEntryNumber addr = (TvmStackEntryNumber) tuple.getElements().get(1); // 32 bytes
            r.add(wc.getNumber() + ":" + addr.getNumber().toString(16).toUpperCase());
        }
        return r;
    }

    /**
     * Get subscription data of the specified plugin
     *
     * @return TvmStackEntryList
     */
    public List<TvmStackEntry> getSubscriptionData(Tonlib tonlib, Address pluginAddress) {

        RunResult result = tonlib.runMethod(pluginAddress, "get_subscription_data");
        if (result.getExit_code() == 0) {
            return result.getStackEntry();
        } else {
            System.err.println("Error executing get_subscription_data. Exit code " + result.getExit_code());
            return null;
        }
    }

    /**
     * @param beneficiary
     * @param amount
     * @param period
     * @param startTime
     * @param timeOut
     * @param lastPaymentTime
     * @param lastRequestTime
     * @param failedAttempts
     * @param subscriptionId  to differ subscriptions to the same beneficiary (acts as a nonce)
     * @return Cell
     */
    public Cell createPluginDataCell(Address wallet, Address beneficiary, BigInteger amount, long period, long startTime, long timeOut, long lastPaymentTime, long lastRequestTime, int failedAttempts, long subscriptionId) {

        CellBuilder cell = CellBuilder.beginCell();
        cell.storeAddress(wallet);
        cell.storeAddress(beneficiary);
        cell.storeCoins(amount);
        cell.storeUint(BigInteger.valueOf(period), 32);
        cell.storeUint(BigInteger.valueOf(startTime), 32);
        cell.storeUint(BigInteger.valueOf(timeOut), 32);
        cell.storeUint(BigInteger.valueOf(lastPaymentTime), 32);
        cell.storeUint(BigInteger.valueOf(lastRequestTime), 32);
        cell.storeUint(BigInteger.valueOf(failedAttempts), 8);
        cell.storeUint(BigInteger.valueOf(subscriptionId), 32);
        return cell.endCell();
    }
}
