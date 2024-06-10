package org.ton.java.smartcontract.wallet.v4;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV4R2Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Builder
@Getter
public class WalletV4R2 implements Contract {

    TweetNaclFast.Signature.KeyPair keyPair;
    long walletId;
    long initialSeqno;


    public static class WalletV4R2Builder {
    }

    public static WalletV4R2Builder builder() {
        return new CustomWalletV4R2Builder();
    }

    private static class CustomWalletV4R2Builder extends WalletV4R2Builder {
        @Override
        public WalletV4R2 build() {
            if (isNull(super.keyPair)) {
                super.keyPair = Utils.generateSignatureKeyPair();
            }
            return super.build();
        }
    }

    private Tonlib tonlib;
    private long wc;

    @Override
    public Tonlib getTonlib() {
        return tonlib;
    }

    @Override
    public long getWorkchain() {
        return wc;
    }

    @Override
    public String getName() {
        return "V4R2";
    }

    @Override
    public Cell createDataCell() {
        return CellBuilder.beginCell()
                .storeUint(initialSeqno, 32)
                .storeUint(walletId, 32)
                .storeBytes(keyPair.getPublicKey())
                .storeUint(0, 1) //plugins dict empty
                .endCell();
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.V4R2.getValue()).
                endCell();
    }

    public Cell createDeployMessage() {
        return CellBuilder.beginCell()
                .storeUint(walletId, 32)
                .storeInt(-1, 32)
                .storeUint(initialSeqno, 32) //seqno
                .endCell();
    }

    public Cell createTransferBody(WalletV4R2Config config) {

        CellBuilder message = CellBuilder.beginCell();

        message.storeUint(config.getWalletId(), 32);

        message.storeUint((config.getValidUntil() == 0) ? Instant.now().getEpochSecond() + 60 : config.getValidUntil(), 32);

        message.storeUint(config.getSeqno(), 32);// msg_seqno

        if (config.getOperation() == 0) {
            Cell order = Message.builder()
                    .info(InternalMessageInfo.builder()
                            .bounce(config.isBounce())
                            .dstAddr(MsgAddressIntStd.builder()
                                    .workchainId(config.getDestination().wc)
                                    .address(config.getDestination().toBigInteger())
                                    .build())
                            .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                            .build())
                    .init(config.getStateInit())
                    .body((isNull(config.getBody()) && nonNull(config.getComment())) ?
                            CellBuilder.beginCell()
                                    .storeUint(0, 32)
                                    .storeString(config.getComment())
                                    .endCell()
                            : config.getBody())
                    .build().toCell();

            message.storeUint(BigInteger.ZERO, 8); // op simple send
            message.storeUint(config.getMode(), 8);
            message.storeRef(order);
        } else if (config.getOperation() == 1) {
            message.storeUint(1, 8); // deploy and install plugin
            message.storeUint(BigInteger.valueOf(config.getNewPlugin().getPluginWc()), 8);
            message.storeCoins(config.getNewPlugin().getAmount()); // plugin balance
            message.storeRef(config.getNewPlugin().getStateInit());
            message.storeRef(config.getNewPlugin().getBody());
        }
        if (config.getOperation() == 2) {
            message.storeUint(2, 8); // install plugin
            message.storeUint(BigInteger.valueOf(config.getDeployedPlugin().getPluginAddress().wc), 8);
            message.storeBytes(config.getDeployedPlugin().getPluginAddress().hashPart);
            message.storeCoins(BigInteger.valueOf(config.getDeployedPlugin().getAmount().longValue()));
            message.storeUint(BigInteger.valueOf(config.getDeployedPlugin().getQueryId()), 64);
            message.endCell();
        }
        if (config.getOperation() == 3) {
            message.storeUint(3, 8); // remove plugin
            message.storeUint(BigInteger.valueOf(config.getDeployedPlugin().getPluginAddress().wc), 8);
            message.storeBytes(config.getDeployedPlugin().getPluginAddress().hashPart);
            message.storeCoins(BigInteger.valueOf(config.getDeployedPlugin().getAmount().longValue()));
            message.storeUint(BigInteger.valueOf(config.getDeployedPlugin().getQueryId()), 64);
            message.endCell();
        }

        return message.endCell();
    }

    /**
     * Deploy wallet without any plugins.
     * One can also deploy plugin separately and later install into the wallet. See installPlugin().
     */

    public ExtMessageInfo deploy() {
        return tonlib.sendRawMessage(prepareDeployMsg().toCell().toBase64());
    }

    /**
     * Deploy wallet without any plugins.
     * One can also deploy plugin separately and later install into the wallet. See installPlugin().
     */
    public Message prepareDeployMsg() {

        Cell body = createDeployMessage();

        return Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .init(getStateInit())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();
    }


    public Cell createPluginStateInit(SubscriptionInfo subscriptionInfo) {
        // code = boc in hex format, result of fift commands:
        //      "subscription-plugin-code.fif" include
        //      2 boc+>B dup Bx. cr
        // boc of subscription contract
        Cell code = CellBuilder.beginCell().fromBoc("B5EE9C7241020F01000262000114FF00F4A413F4BCF2C80B0102012002030201480405036AF230DB3C5335A127A904F82327A128A90401BC5135A0F823B913B0F29EF800725210BE945387F0078E855386DB3CA4E2F82302DB3C0B0C0D0202CD06070121A0D0C9B67813F488DE0411F488DE0410130B048FD6D9E05E8698198FD201829846382C74E2F841999E98F9841083239BA395D497803F018B841083AB735BBED9E702984E382D9C74688462F863841083AB735BBED9E70156BA4E09040B0A0A080269F10FD22184093886D9E7C12C1083239BA39384008646582A803678B2801FD010A65B5658F89659FE4B9FD803FC1083239BA396D9E40E0A04F08E8D108C5F0C708210756E6B77DB3CE00AD31F308210706C7567831EB15210BA8F48305324A126A904F82326A127A904BEF27109FA4430A619F833D078D721D70B3F5260A11BBE8E923036F82370708210737562732759DB3C5077DE106910581047103645135042DB3CE0395F076C2232821064737472BA0A0A0D09011A8E897F821064737472DB3CE0300A006821B39982100400000072FB02DE70F8276F118010C8CB055005CF1621FA0214F40013CB6912CB1F830602948100A032DEC901FB000030ED44D0FA40FA40FA00D31FD31FD31FD31FD31FD307D31F30018021FA443020813A98DB3C01A619F833D078D721D70B3FA070F8258210706C7567228018C8CB055007CF165004FA0215CB6A12CB1F13CB3F01FA02CB00C973FB000E0040C8500ACF165008CF165006FA0214CB1F12CB1FCB1FCB1FCB1FCB07CB1FC9ED54005801A615F833D020D70B078100D1BA95810088D721DED307218100DDBA028100DEBA12B1F2E047D33F30A8AB0FE5855AB4").endCell();
        Cell data = createPluginDataCell(
                getAddress(),
                subscriptionInfo.getBeneficiary(),
                subscriptionInfo.getSubscriptionFee(),
                subscriptionInfo.getPeriod(),
                subscriptionInfo.getStartTime(),
                subscriptionInfo.getTimeOut(),
                subscriptionInfo.getLastPaymentTime(),
                subscriptionInfo.getLastRequestTime(),
                subscriptionInfo.getFailedAttempts(),
                subscriptionInfo.getSubscriptionId());

        return StateInit.builder()
                .code(code)
                .data(data)
                .build().toCell();
    }

    public Cell createPluginBody() {
        return CellBuilder.beginCell() // mgsBody in simple-subscription-plugin.fc is not used
                .storeUint(new BigInteger("706c7567", 16).add(new BigInteger("80000000", 16)), 32) //OP
                .endCell();
    }

    public Cell createPluginSelfDestructBody() {
        return CellBuilder.beginCell()
                .storeUint(0x64737472, 32)
                .endCell();
    }

    public ExtMessageInfo installPlugin(Tonlib tonlib, WalletV4R2Config config) {

        Address ownAddress = getAddress();
        config.setOperation(2);
        Cell body = createTransferBody(config); //seqno only needed
        Cell extMsg = MsgUtils.createExternalMessageWithSignedBody(keyPair, ownAddress, getStateInit(), body).toCell();

        return tonlib.sendRawMessage(extMsg.toBase64());
    }

    public ExtMessageInfo uninstallPlugin(WalletV4R2Config config) {

        Address ownAddress = getAddress();
        config.setOperation(3);
        Cell body = createTransferBody(config); //seqno only needed
        Cell extMsg = MsgUtils.createExternalMessageWithSignedBody(keyPair, ownAddress, getStateInit(), body).toCell();

        return tonlib.sendRawMessage(extMsg.toBase64());
    }

    /**
     * @return subwallet-id long
     */
    public long getWalletId() {
        RunResult result = tonlib.runMethod(getAddress(), "get_subwallet_id");
        TvmStackEntryNumber subWalletId = (TvmStackEntryNumber) result.getStack().get(0);

        return subWalletId.getNumber().longValue();
    }

    public byte[] getPublicKey() {
        RunResult result = tonlib.runMethod(getAddress(), "get_public_key");
        TvmStackEntryNumber pubKey = (TvmStackEntryNumber) result.getStack().get(0);

        return pubKey.getNumber().toByteArray();
    }

    /**
     * @param pluginAddress Address
     * @return boolean
     */
    public boolean isPluginInstalled(Address pluginAddress) {
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
    public List<String> getPluginsList() {
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
    public SubscriptionInfo getSubscriptionData(Address pluginAddress) {

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

        return CellBuilder.beginCell()
                .storeAddress(wallet)
                .storeAddress(beneficiary)
                .storeCoins(amount)
                .storeUint(BigInteger.valueOf(period), 32)
                .storeUint(BigInteger.valueOf(startTime), 32)
                .storeUint(BigInteger.valueOf(timeOut), 32)
                .storeUint(BigInteger.valueOf(lastPaymentTime), 32)
                .storeUint(BigInteger.valueOf(lastRequestTime), 32)
                .storeUint(BigInteger.valueOf(failedAttempts), 8)
                .storeUint(BigInteger.valueOf(subscriptionId), 32)
                .endCell();
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
     * @param config WalletV4R2Config
     */

    public ExtMessageInfo send(WalletV4R2Config config) {
        return tonlib.sendRawMessage(prepareExternalMsg(config).toCell().toBase64());
    }

    public Message prepareExternalMsg(WalletV4R2Config config) {
        Cell body = createTransferBody(config);
        return MsgUtils.createExternalMessageWithSignedBody(keyPair, getAddress(), null, body);
    }
}
