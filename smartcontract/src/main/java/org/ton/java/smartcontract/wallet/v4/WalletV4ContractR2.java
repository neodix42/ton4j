package org.ton.java.smartcontract.wallet.v4;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.DeployedPlugin;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.NewPlugin;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.*;

import static java.util.Objects.isNull;

public class WalletV4ContractR2 implements WalletContract {

    Options options;
    Address address;

    /**
     * @param options Options
     */
    public WalletV4ContractR2(Options options) {
        this.options = options;
        options.code = Cell.fromBoc(WalletCodes.V4R2.getValue());
        if (isNull(options.walletId)) {
            options.walletId = 698983191 + options.wc;
        }
    }

    @Override
    public String getName() {
        return "V4R2";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public Address getAddress() {
        if (isNull(address)) {
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
            long timestamp = (long) Math.floor(date.getTime() / 1e3);
            message.storeUint(BigInteger.valueOf(timestamp + 60L), 32);
        }

        message.storeUint(BigInteger.valueOf(seqno), 32);

        if (!withoutOp) {
            message.storeUint(BigInteger.ZERO, 8); // op
        }

        return message.endCell();
    }

    /**
     * Deploy and install/assigns subscription plugin.
     * One can also deploy plugin separately and later install into the wallet. See installPlugin().
     *
     * @param params NewPlugin
     */
    public void deployAndInstallPlugin(Tonlib tonlib, NewPlugin params) {

        Cell signingMessage = createSigningMessage(params.seqno, true);
        signingMessage.bits.writeUint(BigInteger.ONE, 8); // op
        signingMessage.bits.writeInt(BigInteger.valueOf(params.pluginWc), 8);
        signingMessage.bits.writeCoins(params.amount); // plugin balance
        signingMessage.refs.add(params.stateInit);
        signingMessage.refs.add(params.body);
        ExternalMessage extMsg = createExternalMessage(signingMessage, params.secretKey, params.seqno, false);

        tonlib.sendRawMessage(extMsg.message.toBocBase64(false));
    }

    public Cell createPluginStateInit() {
        // code = boc in hex format, result of fift commands:
        //      "subscription-plugin-code.fif" include
        //      2 boc+>B dup Bx. cr
        // boc of subscription contract
        Cell code = Cell.fromBoc("B5EE9C7241020F01000262000114FF00F4A413F4BCF2C80B0102012002030201480405036AF230DB3C5335A127A904F82327A128A90401BC5135A0F823B913B0F29EF800725210BE945387F0078E855386DB3CA4E2F82302DB3C0B0C0D0202CD06070121A0D0C9B67813F488DE0411F488DE0410130B048FD6D9E05E8698198FD201829846382C74E2F841999E98F9841083239BA395D497803F018B841083AB735BBED9E702984E382D9C74688462F863841083AB735BBED9E70156BA4E09040B0A0A080269F10FD22184093886D9E7C12C1083239BA39384008646582A803678B2801FD010A65B5658F89659FE4B9FD803FC1083239BA396D9E40E0A04F08E8D108C5F0C708210756E6B77DB3CE00AD31F308210706C7567831EB15210BA8F48305324A126A904F82326A127A904BEF27109FA4430A619F833D078D721D70B3F5260A11BBE8E923036F82370708210737562732759DB3C5077DE106910581047103645135042DB3CE0395F076C2232821064737472BA0A0A0D09011A8E897F821064737472DB3CE0300A006821B39982100400000072FB02DE70F8276F118010C8CB055005CF1621FA0214F40013CB6912CB1F830602948100A032DEC901FB000030ED44D0FA40FA40FA00D31FD31FD31FD31FD31FD307D31F30018021FA443020813A98DB3C01A619F833D078D721D70B3FA070F8258210706C7567228018C8CB055007CF165004FA0215CB6A12CB1F13CB3F01FA02CB00C973FB000E0040C8500ACF165008CF165006FA0214CB1F12CB1FCB1FCB1FCB1FCB07CB1FC9ED54005801A615F833D020D70B078100D1BA95810088D721DED307218100DDBA028100DEBA12B1F2E047D33F30A8AB0FE5855AB4");
        Cell data = createPluginDataCell(
                getAddress(),
                options.getSubscriptionConfig().getBeneficiary(),
                options.getSubscriptionConfig().getSubscriptionFee(),
                options.getSubscriptionConfig().getPeriod(),
                options.getSubscriptionConfig().getStartTime(),
                options.getSubscriptionConfig().getTimeOut(),
                options.getSubscriptionConfig().getLastPaymentTime(),
                options.getSubscriptionConfig().getLastRequestTime(),
                options.getSubscriptionConfig().getFailedAttempts(),
                options.getSubscriptionConfig().getSubscriptionId());
        return createStateInit(code, data);
    }

    public Cell createPluginBody() {
        CellBuilder body = CellBuilder.beginCell(); // mgsBody in simple-subscription-plugin.fc is not used
        body.storeUint(new BigInteger("706c7567", 16).add(new BigInteger("80000000", 16)), 32); //OP
        return body.endCell();
    }

    public Cell createPluginSelfDestructBody() {
        return CellBuilder.beginCell().storeUint(0x64737472, 32).endCell();
    }

    /**
     * @param params    DeployedPlugin,
     * @param isInstall boolean install or uninstall
     */
    ExternalMessage setPlugin(DeployedPlugin params, boolean isInstall) {

        Cell signingMessage = createSigningMessage(params.seqno, true);
        signingMessage.bits.writeUint(isInstall ? BigInteger.TWO : BigInteger.valueOf(3), 8); // op
        signingMessage.bits.writeInt(BigInteger.valueOf(params.pluginAddress.wc), 8);
        signingMessage.bits.writeBytes(params.pluginAddress.hashPart);
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
     * @return subwallet-id long
     */
    public long getWalletId(Tonlib tonlib) {

        Address myAddress = getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_subwallet_id");
        TvmStackEntryNumber subWalletId = (TvmStackEntryNumber) result.getStack().get(0);

        return subWalletId.getNumber().longValue();
    }

    public byte[] getPublicKey(Tonlib tonlib) {
        Address myAddress = getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_public_key");
        TvmStackEntryNumber pubKey = (TvmStackEntryNumber) result.getStack().get(0);

        return pubKey.getNumber().toByteArray();
    }

    /**
     * @param pluginAddress Address
     * @return boolean
     */
    public boolean isPluginInstalled(Tonlib tonlib, Address pluginAddress) {
        String hashPart = new BigInteger(pluginAddress.hashPart).toString();

        Address myAddress = getAddress();

        Deque<String> stack = new ArrayDeque<>();
        stack.offer("[num, " + pluginAddress.wc + "]");
        stack.offer("[num, " + hashPart + "]");

        RunResult result = tonlib.runMethod(myAddress, "is_plugin_installed", stack);
        TvmStackEntryNumber resultNumber = (TvmStackEntryNumber) result.getStack().get(0);

        return resultNumber.getNumber().longValue() != 0;
    }

    /**
     * @return List<String> plugins addresses
     */
    public List<String> getPluginsList(Tonlib tonlib) {
        List<String> r = new ArrayList<>();
        Address myAddress = getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_plugin_list");
        TvmStackEntryList list = (TvmStackEntryList) result.getStack().get(0);
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
    public SubscriptionInfo getSubscriptionData(Tonlib tonlib, Address pluginAddress) {

        RunResult result = tonlib.runMethod(pluginAddress, "get_subscription_data");
        if (result.getExit_code() == 0) {
            return parseSubscriptionData(result.getStack());
        } else {
            throw new Error("Error executing get_subscription_data. Exit code " + result.getExit_code());

        }
    }

    public Cell createPluginDataCell(Address wallet,
                                     Address beneficiary,
                                     BigInteger amount,
                                     long period,
                                     long startTime,
                                     long timeOut,
                                     long lastPaymentTime,
                                     long lastRequestTime,
                                     long failedAttempts,
                                     long subscriptionId) {

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

    private SubscriptionInfo parseSubscriptionData(List subscriptionData) {
        TvmStackEntryTuple walletAddr = (TvmStackEntryTuple) subscriptionData.get(0);
        TvmStackEntryNumber wc = (TvmStackEntryNumber) walletAddr.getTuple().getElements().get(0);
        TvmStackEntryNumber hash = (TvmStackEntryNumber) walletAddr.getTuple().getElements().get(1);
        TvmStackEntryTuple beneficiaryAddr = (TvmStackEntryTuple) subscriptionData.get(1);
        TvmStackEntryNumber beneficiaryAddrWc = (TvmStackEntryNumber) beneficiaryAddr.getTuple().getElements().get(0);
        TvmStackEntryNumber beneficiaryAddrHash = (TvmStackEntryNumber) beneficiaryAddr.getTuple().getElements().get(1);
        TvmStackEntryNumber amount = (TvmStackEntryNumber) subscriptionData.get(2);
        TvmStackEntryNumber period = (TvmStackEntryNumber) subscriptionData.get(3);
        TvmStackEntryNumber startTime = (TvmStackEntryNumber) subscriptionData.get(4);
        TvmStackEntryNumber timeOut = (TvmStackEntryNumber) subscriptionData.get(5);
        TvmStackEntryNumber lastPaymentTime = (TvmStackEntryNumber) subscriptionData.get(6);
        TvmStackEntryNumber lastRequestTime = (TvmStackEntryNumber) subscriptionData.get(7);

        long now = System.currentTimeMillis() / 1000;
        boolean isPaid = ((now - lastPaymentTime.getNumber().longValue()) < period.getNumber().longValue());
        boolean paymentReady = !isPaid & ((now - lastRequestTime.getNumber().longValue()) > timeOut.getNumber().longValue());

        TvmStackEntryNumber failedAttempts = (TvmStackEntryNumber) subscriptionData.get(8);
        TvmStackEntryNumber subscriptionId = (TvmStackEntryNumber) subscriptionData.get(9);

        return SubscriptionInfo.builder()
                .walletAddress(Address.of(wc.getNumber() + ":" + hash.getNumber().toString(16)))
                .beneficiary(Address.of(beneficiaryAddrWc.getNumber() + ":" + beneficiaryAddrHash.getNumber().toString(16)))
                .subscriptionFee(amount.getNumber())
                .period(period.getNumber().longValue())
                .startTime(startTime.getNumber().longValue())
                .timeOut(timeOut.getNumber().longValue())
                .lastPaymentTime(lastPaymentTime.getNumber().longValue())
                .lastRequestTime(lastRequestTime.getNumber().longValue())
                .isPaid(isPaid)
                .isPaymentReady(paymentReady)
                .failedAttempts(failedAttempts.getNumber().longValue())
                .subscriptionId(subscriptionId.getNumber().longValue())
                .build();
    }


    /**
     * Sends amount of nano toncoins to destination address using auto-fetched seqno without the body and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno);
        tonlib.sendRawMessage(msg.message.toBocBase64(false));
    }

    /**
     * Sends amount of nano toncoins to destination address using specified seqno with the body and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param body               byte[]
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, byte[] body) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body);
        tonlib.sendRawMessage(msg.message.toBocBase64(false));
    }

    /**
     * Sends amount of nano toncoins to destination address using specified seqno with the body and specified send-mode
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param body               byte[]
     * @param sendMode           byte
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, byte[] body, byte sendMode) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body, sendMode);
        tonlib.sendRawMessage(msg.message.toBocBase64(false));
    }

    /**
     * Sends amount of nano toncoins to destination address using auto-fetched seqno with the body and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param body               byte[]
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, byte[] body) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body);
        tonlib.sendRawMessage(msg.message.toBocBase64(false));
    }

    /**
     * Sends amount of nano toncoins to destination address using specified seqno with the comment and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param seqno              long
     * @param comment            String
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno, String comment) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, CellBuilder.beginCell().storeUint(0, 32).storeString(comment).endCell());
        tonlib.sendRawMessage(msg.message.toBocBase64(false));
    }

    /**
     * Sends amount of nano toncoins to destination address using specified seqno without the comment and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param seqno              long
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, long seqno) {
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno);
        tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));
    }

    /**
     * Sends amount of nano toncoins to destination address using auto-fetched seqno without the body and default send-mode 3
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param comment            String
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, String comment) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, CellBuilder.beginCell().storeUint(0, 32).storeString(comment).endCell());
        tonlib.sendRawMessage(msg.message.toBocBase64(false));
    }

    /**
     * Sends amount of nano toncoins to destination address using auto-fetched seqno with the body and specified send-mode
     *
     * @param tonlib             Tonlib
     * @param secretKey          byte[]
     * @param destinationAddress Address
     * @param amount             BigInteger
     * @param body               byte[]
     * @param sendMode           byte
     */
    public void sendTonCoins(Tonlib tonlib, byte[] secretKey, Address destinationAddress, BigInteger amount, byte[] body, byte sendMode) {
        long seqno = getSeqno(tonlib);
        ExternalMessage msg = createTransferMessage(secretKey, destinationAddress, amount, seqno, body, sendMode);
        tonlib.sendRawMessage(msg.message.toBocBase64(false));
    }
}
