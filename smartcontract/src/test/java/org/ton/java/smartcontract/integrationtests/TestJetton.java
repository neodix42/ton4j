package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.token.ft.JettonMinter;
import org.ton.java.smartcontract.token.ft.JettonWallet;
import org.ton.java.smartcontract.types.*;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.smartcontract.wallet.v3.WalletV3ContractR1;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.FullAccountState;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

@Slf4j
@RunWith(JUnit4.class)
public class TestJetton {
    public static final String NEW_ADMIN2 = "EQB6-6po0yspb68p7RRetC-hONAz-JwxG9514IEOKw_llXd5";
    public static final String WALLET_ADDRESS_2 = "EQBGpCSFJpAb1guZHVWIO8b_8g0e8yxp2ZfZWcTXvTjvvyFd";
    static TweetNaclFast.Signature.KeyPair keyPairAdmin1;
    static TweetNaclFast.Signature.KeyPair keyPair2;
    static WalletV3ContractR1 adminWallet;
    static WalletV3ContractR1 wallet2;
    static Tonlib tonlib = Tonlib.builder().testnet(true).build();

    private static Address walletAddress;

    @BeforeClass
    public static void setUpClass() throws InterruptedException {
        String predefinedSecretKey = "9a98f996e91dea81560cd539f725ef01456705220ca2eb314ac547ed21bbc161235dc8daef9f3e9282963356a668b4b71329ad4743dc709674aec4a826fc750b";
//        String predefinedSecretKey = "";
//        test-wallet init address 0QDbgKZ6Xd3u-q6PuDHbZTwFiBv1N2-FHIJuQ8xzd27X6tw-
//        raw address 0:db80a67a5dddeefaae8fb831db653c05881bf5376f851c826e43cc73776ed7ea

        if (StringUtils.isEmpty(predefinedSecretKey)) {
            keyPairAdmin1 = Utils.generateSignatureKeyPair();
        } else {
            keyPairAdmin1 = Utils.generateSignatureKeyPairFromSeed(Utils.hexToBytes(predefinedSecretKey));
        }

        log.info("pubKey {}, prvKey {}", Utils.bytesToHex(keyPairAdmin1.getPublicKey()), Utils.bytesToHex(keyPairAdmin1.getSecretKey()));

        Options options = Options.builder()
                .publicKey(keyPairAdmin1.getPublicKey())
                .wc(0L)
                .build();

        Wallet walletcontract = new Wallet(WalletVersion.v3R1, options);
        adminWallet = walletcontract.create();

        InitExternalMessage msg = adminWallet.createInitExternalMessage(keyPairAdmin1.getSecretKey());
        Address address = msg.address;
        walletAddress = address;

        String nonBounceableAddress = address.toString(true, true, false, true);
        String bounceableAddress = address.toString(true, true, true, true);

        log.info("\nNon-bounceable address (for init): {}\nBounceable address (for later access): {}\nraw: {}",
                nonBounceableAddress,
                bounceableAddress,
                address.toString(false));

        if (StringUtils.isEmpty(predefinedSecretKey)) {
            BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
            log.info("new wallet balance {}", Utils.formatNanoValue(balance));
            // deploy new wallet
            tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));
        }

        long seqno = adminWallet.getSeqno(tonlib);
        log.info("wallet seqno {}", seqno);
    }

    private void createWallet2() throws InterruptedException {
        keyPair2 = Utils.generateSignatureKeyPair();

        log.info("wallet2 pubKey {}, prvKey {}", Utils.bytesToHex(keyPair2.getPublicKey()), Utils.bytesToHex(keyPair2.getSecretKey()));

        Options options = Options.builder()
                .publicKey(keyPair2.getPublicKey())
                .wc(0L)
                .build();

        Wallet walletcontract = new Wallet(WalletVersion.v3R1, options);
        wallet2 = walletcontract.create();
        log.info("wallet 2 address {}", wallet2.getAddress().toString(true, true, true));
        InitExternalMessage msg = wallet2.createInitExternalMessage(keyPair2.getSecretKey());
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(wallet2.getAddress().toString(true, true, false)), Utils.toNano(1));
        log.info("new wallet balance {}", Utils.formatNanoValue(balance));
        // deploy new wallet
        tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));
    }

    @Test
    public void testJetton() throws InterruptedException {

        JettonMinter minter = delployMinter();
        Utils.sleep(15);
        getMinterInfo(minter);

        // sequential calls to min() sum up to totalSupply;
        minter.mint(tonlib, adminWallet, adminWallet.getAddress(), Utils.toNano(0.05), Utils.toNano(0.04), Utils.toNano(100500), keyPairAdmin1);

        log.info("jetton total supply {}", minter.getTotalSupply(tonlib));

        //owner of adminWallet holds his jettons on jettonWallet
        Address adminJettonWalletAddress = minter.getJettonWalletAddress(tonlib, adminWallet.getAddress());
        log.info("admin JettonWalletAddress {}", adminJettonWalletAddress.toString(true, true, true));

        JettonWallet adminJettonWallet = getJettonWalletInfo(adminJettonWalletAddress);

        editMinterContent(minter, "http://localhost/nft-marketplace/my_collection.1");
        Utils.sleep(20);
        getMinterInfo(minter);

        log.info("newAdmin {}", Address.of(NEW_ADMIN2).toString(false));
        changeMinterAdmin(minter, Address.of(NEW_ADMIN2));
        Utils.sleep(20);
        getMinterInfo(minter);

        createWallet2();

        Utils.sleep(15);
        FullAccountState wallet2State = tonlib.getAccountState(Address.of(wallet2.getAddress()));

        log.info("wallet 2 balance " + wallet2State.getBalance());
        //transfer from admin to WALLET2_ADDRESS by sending transfer request to admin's jetton wallet
        transfer(adminWallet, adminJettonWallet.getAddress(), Address.of(wallet2.getAddress()), Utils.toNano(555), keyPairAdmin1);
        Utils.sleep(20);
        log.info("changed admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance(tonlib)));

        //wallet 2 after received jettons, has JettonWallet assigned
        getJettonWalletInfo(minter.getJettonWalletAddress(tonlib, wallet2.getAddress()));

        burn(adminWallet, adminJettonWallet.getAddress(), Utils.toNano(444), walletAddress, keyPairAdmin1);
        Utils.sleep(20);
        log.info("changed admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance(tonlib)));
    }

    private JettonMinter delployMinter() {

        Options options = Options.builder()
                .adminAddress(walletAddress)
                .jettonContentUri("https://ton.org/jetton.json")
                .jettonWalletCodeHex(JettonWallet.JETTON_WALLET_CODE_HEX)
                .wc(0L)
                .build();

        Wallet jettonMinter = new Wallet(WalletVersion.jettonMinter, options);
        JettonMinter minter = jettonMinter.create();
        log.info("jetton minter address {}", minter.getAddress().toString(true, true, true));
        minter.deploy(tonlib, adminWallet, Utils.toNano(0.05), keyPairAdmin1);

        return minter;
    }

    private JettonWallet getJettonWalletInfo(Address address) {
        Options optionsJettonWallet = Options.builder()
                .address(address)
                .build();

        Wallet wallet = new Wallet(WalletVersion.jettonWallet, optionsJettonWallet);
        JettonWallet jettonWallet = wallet.create();

        JettonWalletData data = jettonWallet.getData(tonlib);
        log.info("jettonWalletData {}", data);
        log.info("balance in jettons {}", Utils.formatNanoValue(data.getBalance()));
        return jettonWallet;
    }

    private void getMinterInfo(JettonMinter minter) {
        JettonMinterData data = minter.getJettonData(tonlib);
        log.info("JettonMinterData {}", data);
        log.info("minter adminAddress {}", data.getAdminAddress().toString(true, true, true));
        log.info("minter totalSupply {}", Utils.formatNanoValue(data.getTotalSupply()));
    }


    private void editMinterContent(JettonMinter minter, String newUriContent) {
        log.info("edit content");
        long seqno = adminWallet.getSeqno(tonlib);

        ExternalMessage extMsg = adminWallet.createTransferMessage(
                keyPairAdmin1.getSecretKey(),
                minter.getAddress(),
                Utils.toNano(0.05),
                seqno,
                minter.createEditContentBody(newUriContent, 0));

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }

    private void changeMinterAdmin(JettonMinter minter, Address newAdmin) {
        log.info("change admin");
        long seqno = adminWallet.getSeqno(tonlib);

        ExternalMessage extMsg = adminWallet.createTransferMessage(
                keyPairAdmin1.getSecretKey(),
                minter.getAddress(),
                Utils.toNano(0.05),
                seqno,
                minter.createChangeAdminBody(0, newAdmin));

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }

    /**
     * @param jettonWalletAddress Address
     * @param toAddress           Address
     * @param jettonAmount        BigInteger
     * @param keyPair             KeyPair
     */
    private void transfer(WalletContract admin, Address jettonWalletAddress, Address toAddress, BigInteger jettonAmount, TweetNaclFast.Signature.KeyPair keyPair) {
        log.info("transfer");
        long seqno = admin.getSeqno(tonlib);

        ExternalMessage extMsg = admin.createTransferMessage(
                keyPair.getSecretKey(),
                Address.of(jettonWalletAddress),
                Utils.toNano(0.05),
                seqno,
                JettonWallet.createTransferBody(
                        0,
                        jettonAmount,
                        Address.of(toAddress), // destination
                        admin.getAddress(), // response address
                        Utils.toNano("0.01"),
                        "gift".getBytes()
                ));

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }

    private void burn(WalletContract admin, Address jettonWalletAddress, BigInteger jettonAmount, Address responseAddress, TweetNaclFast.Signature.KeyPair keyPair) {
        log.info("burn");
        long seqno = admin.getSeqno(tonlib);

        ExternalMessage extMsg = admin.createTransferMessage(
                keyPair.getSecretKey(),
                Address.of(jettonWalletAddress),
                Utils.toNano(0.05),
                seqno,
                JettonWallet.createBurnBody(
                        0,
                        jettonAmount,
                        responseAddress
                ));

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }
}
