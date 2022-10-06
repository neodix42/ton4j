package org.ton.java.smartcontract.unittests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.types.InitExternalMessage;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.v4.WalletV4ContractR2;
import org.ton.java.tonlib.types.TvmStackEntry;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.tonlib.types.TvmStackEntryTuple;
import org.ton.java.utils.Utils;

import java.util.List;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletsV4 {

    /**
     * test new-wallet-v4r2.fc
     * >fift -s new-wallet-v4r2.fc 0 698983191
     */
    @Test
    public void testNewWalletV4r2() {

        byte[] publicKey = Utils.hexToBytes("82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
        byte[] secretKey = Utils.hexToBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);
//        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
//
//        log.info("secKey {}", Utils.bytesToHex(keyPair.getSecretKey()));

        Options options = new Options();
        options.publicKey = keyPair.getPublicKey();
        options.wc = 0L;

        Wallet wallet = new Wallet(WalletVersion.v4R2, options);
        WalletV4ContractR2 contract = wallet.create();

        InitExternalMessage msg = contract.createInitExternalMessage(keyPair.getSecretKey());
        Address walletAddress = msg.address;

        String my = "Creating new advanced wallet V4 with plugins in workchain " + options.wc + "\n" +
                "with unique wallet id " + options.walletId + "\n" +
                "Loading private key from file new-wallet.pk" + "\n" +
                "StateInit: " + msg.stateInit.print() + "\n" +
                "new wallet address = " + walletAddress.toString(false) + "\n" +
                "(Saving address to file new-wallet.addr)" + "\n" +
                "Non-bounceable address (for init): " + walletAddress.toString(true, true, false, true) + "\n" +
                "Bounceable address (for later access): " + walletAddress.toString(true, true, true, true) + "\n" +
                "signing message: " + msg.signingMessage.print() + "\n" +
                "External message for initialization is " + msg.message.print() + "\n" +
                Utils.bytesToHex(msg.message.toBoc(false)).toUpperCase() + "\n" +
                "(Saved wallet creating query to file new-wallet-query.boc)" + "\n";
        log.info(my);

        // send some toincoin to non-bounceable address first
/* proceed manually

        // deploy wallet-v4
        Tonlib tonlib = Tonlib.builder().build();
        String base64boc = Utils.bytesToBase64(msg.message.toBoc(false));
        log.info(base64boc);
        tonlib.sendRawMessage(base64boc);

        // breakpoint, pause

        long walletCurrentSeqno = contract.getSeqno(tonlib);
        log.info("seqno: {}", walletCurrentSeqno);
        log.info("subWalletId: {}", contract.getWalletId(tonlib));
        log.info("pubKey: {}", contract.getPublicKey(tonlib));
        log.info("pluginsList: {}", contract.getPluginsList(tonlib));

        // uninstall plugin -- start
//        DeployedPlugin deployedPlugin = DeployedPlugin.builder()
//                .seqno(walletCurrentSeqno)
//                .amount(BigInteger.valueOf(100000000)) // 0.1 toncoin
//                .pluginAddress(new Address(contract.getPluginsList(tonlib).get(0)))
//                .secretKey(keyPair.getSecretKey())
//                .queryId(0)
//                .build();
//
//        ExternalMessage extMsgRemovePlugin = contract.removePlugin(deployedPlugin);
//        String extMsgRemovePluginBase64boc = Utils.bytesToBase64(extMsgRemovePlugin.message.toBoc(false));
//        tonlib.sendRawMessage(extMsgRemovePluginBase64boc);
//        log.info("pluginsList: {}", contract.getPluginsList(tonlib));

        // uninstall plugin -- end

        // create subscription, i.e. create and deploy plugin ------- start -----------------

        // code = boc in hex format, result of fift commands:
        //      "subscription-plugin-code.fif" include
        //      2 boc+>B dup Bx. cr

        Address beneficiaryWallet = new Address("kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka");

        Cell code = Cell.fromBoc("B5EE9C7241020F01000262000114FF00F4A413F4BCF2C80B0102012002030201480405036AF230DB3C5335A127A904F82327A128A90401BC5135A0F823B913B0F29EF800725210BE945387F0078E855386DB3CA4E2F82302DB3C0B0C0D0202CD06070121A0D0C9B67813F488DE0411F488DE0410130B048FD6D9E05E8698198FD201829846382C74E2F841999E98F9841083239BA395D497803F018B841083AB735BBED9E702984E382D9C74688462F863841083AB735BBED9E70156BA4E09040B0A0A080269F10FD22184093886D9E7C12C1083239BA39384008646582A803678B2801FD010A65B5658F89659FE4B9FD803FC1083239BA396D9E40E0A04F08E8D108C5F0C708210756E6B77DB3CE00AD31F308210706C7567831EB15210BA8F48305324A126A904F82326A127A904BEF27109FA4430A619F833D078D721D70B3F5260A11BBE8E923036F82370708210737562732759DB3C5077DE106910581047103645135042DB3CE0395F076C2232821064737472BA0A0A0D09011A8E897F821064737472DB3CE0300A006821B39982100400000072FB02DE70F8276F118010C8CB055005CF1621FA0214F40013CB6912CB1F830602948100A032DEC901FB000030ED44D0FA40FA40FA00D31FD31FD31FD31FD31FD307D31F30018021FA443020813A98DB3C01A619F833D078D721D70B3FA070F8258210706C7567228018C8CB055007CF165004FA0215CB6A12CB1F13CB3F01FA02CB00C973FB000E0040C8500ACF165008CF165006FA0214CB1F12CB1FCB1FCB1FCB1FCB07CB1FC9ED54005801A615F833D020D70B078100D1BA95810088D721DED307218100DDBA028100DEBA12B1F2E047D33F30A8AB0FE5855AB4");
        Cell data = contract.createPluginDataCell(
                walletAddress, // wallet-v4, my wallet (payer, me)
                beneficiaryWallet, // external service provider (payee)
                Utils.toNano(1), // 1 toncoin amount to charge for a period?
                3600 * 2,
                123,
                3600 * 24,
                0,
                0,
                0,
                12345);

        log.info("data {}", Utils.bytesToHex(data.toBoc(false)));
        log.info("data {}", data.print());

        Cell body = new Cell(); // mgsBody in simple-subscription-plugin.fc is not used
        body.bits.writeUint(BigInteger.valueOf(0x706c7567 + 0x80000000), 32);

        Cell stateInit = contract.createStateInit(code, data);

        log.info("stateInit {}", Utils.bytesToHex(stateInit.toBoc(false)));
        log.info("stateInit {}", stateInit.print());

        NewPlugin plugin = NewPlugin.builder()
                .secretKey(keyPair.getSecretKey())
                .seqno(walletCurrentSeqno)
                .pluginWc(options.wc)
                .amount(Utils.toNano(10)) // plugin balance, will be taken from wallet-v4
                .stateInit(stateInit)
                .body(body)
                .build();

        // deploy plugin
        ExternalMessage newPluginExtMsg = contract.deployAndInstallPlugin(plugin);
        String newPluginExtMsgBase64 = Utils.bytesToBase64(newPluginExtMsg.message.toBoc(false));
        tonlib.sendRawMessage(newPluginExtMsgBase64);

        // create and deploy plugin -------- end ----------------


        // get plugin list
        List<String> plugins = contract.getPluginsList(tonlib);
        log.info("pluginsList: {}", plugins.get(0));

        Address pluginAddress = new Address(plugins.get(0));
        log.info("pluginAddress {}", pluginAddress.toString(false));

        List<TvmStackEntry> subscriptionData = contract.getSubscriptionData(tonlib, pluginAddress);

        showSubscriptionData(subscriptionData);

        // Collect service fee. External provider sends an external message to customer's plugin's address (derived from wallet-v4 plugin list)

        Cell header = Contract.createExternalMessageHeader(pluginAddress);
        Cell extMessage = Contract.createCommonMsgInfo(header, null, null); // dummy external message, only destination address is relevant
        String extMessageBase64boc = Utils.bytesToBase64(extMessage.toBoc(false));
        tonlib.sendRawMessage(extMessageBase64boc);
        */
    }

    private void showSubscriptionData(List<TvmStackEntry> subscriptionData) {
        TvmStackEntryTuple walletAddr = (TvmStackEntryTuple) subscriptionData.get(0);
        TvmStackEntryNumber wc = (TvmStackEntryNumber) walletAddr.getTuple().getElements().get(0);
        TvmStackEntryNumber hash = (TvmStackEntryNumber) walletAddr.getTuple().getElements().get(1);
        log.info("walletAddr: {}:{}", wc.getNumber(), hash.getNumber().toString(16));

        TvmStackEntryTuple beneficiaryAddr = (TvmStackEntryTuple) subscriptionData.get(1);
        TvmStackEntryNumber beneficiaryAddrWc = (TvmStackEntryNumber) beneficiaryAddr.getTuple().getElements().get(0);
        TvmStackEntryNumber beneficiaryAddrHash = (TvmStackEntryNumber) beneficiaryAddr.getTuple().getElements().get(1);
        log.info("walletAddr: {}:{}", beneficiaryAddrWc.getNumber(), beneficiaryAddrHash.getNumber().toString(16));

        TvmStackEntryNumber amount = (TvmStackEntryNumber) subscriptionData.get(2);
        log.info("amount: {}", amount.getNumber());

        TvmStackEntryNumber period = (TvmStackEntryNumber) subscriptionData.get(3);
        log.info("period: {}", period.getNumber());

        TvmStackEntryNumber startTime = (TvmStackEntryNumber) subscriptionData.get(4);
        log.info("startTime: {}", startTime.getNumber());

        TvmStackEntryNumber timeOut = (TvmStackEntryNumber) subscriptionData.get(5);
        log.info("timeOut: {}", timeOut.getNumber());

        TvmStackEntryNumber lastPaymentTime = (TvmStackEntryNumber) subscriptionData.get(6);
        log.info("lastPaymentTime: {}", lastPaymentTime.getNumber());

        TvmStackEntryNumber lastRequestTime = (TvmStackEntryNumber) subscriptionData.get(7);
        log.info("lastRequestTime: {}", lastRequestTime.getNumber());

        TvmStackEntryNumber failedAttempts = (TvmStackEntryNumber) subscriptionData.get(8);
        log.info("failedAttempts: {}", failedAttempts.getNumber());

        TvmStackEntryNumber subscriptionId = (TvmStackEntryNumber) subscriptionData.get(9);
        log.info("subscriptionId: {}", subscriptionId.getNumber());
    }
}
