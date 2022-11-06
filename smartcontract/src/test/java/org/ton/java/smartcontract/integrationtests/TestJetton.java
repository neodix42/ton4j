package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.token.ft.JettonMinter;
import org.ton.java.smartcontract.token.ft.JettonWallet;
import org.ton.java.smartcontract.types.*;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.v3.WalletV3ContractR1;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

@Slf4j
@RunWith(JUnit4.class)
public class TestJetton {

    public static final String WALLET2_ADDRESS = "EQB6-6po0yspb68p7RRetC-hONAz-JwxG9514IEOKw_llXd5";
    public static final String JETTON_WALLET_ADDRESS = "EQBGpCSFJpAb1guZHVWIO8b_8g0e8yxp2ZfZWcTXvTjvvyFd";


    static TweetNaclFast.Signature.KeyPair keyPair;
    static WalletV3ContractR1 wallet;
    static Tonlib tonlib = Tonlib.builder()
            .testnet(true)
//            .verbosityLevel(VerbosityLevel.DEBUG)
            .build();

    @BeforeClass
    public static void setUpClass() throws InterruptedException {
        String predefinedSecretKey = "9a98f996e91dea81560cd539f725ef01456705220ca2eb314ac547ed21bbc161235dc8daef9f3e9282963356a668b4b71329ad4743dc709674aec4a826fc750b";
//        String predefinedSecretKey = "";
//        test-wallet init address 0QDbgKZ6Xd3u-q6PuDHbZTwFiBv1N2-FHIJuQ8xzd27X6tw-
//        raw address 0:db80a67a5dddeefaae8fb831db653c05881bf5376f851c826e43cc73776ed7ea

        if (StringUtils.isEmpty(predefinedSecretKey)) {
            keyPair = Utils.generateSignatureKeyPair();
        } else {
            keyPair = Utils.generateSignatureKeyPairFromSeed(Utils.hexToBytes(predefinedSecretKey));
        }

        log.info("pubKey {}, prvKey {}", Utils.bytesToHex(keyPair.getPublicKey()), Utils.bytesToHex(keyPair.getSecretKey()));

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .wc(0L)
                .build();

        Wallet walletcontract = new Wallet(WalletVersion.v3R1, options);
        wallet = walletcontract.create();

        InitExternalMessage msg = wallet.createInitExternalMessage(keyPair.getSecretKey());
        Address address = msg.address;

        String nonBounceableAddress = address.toString(true, true, false, true);
        String bounceableAddress = address.toString(true, true, true, true);

        log.info("\nNon-bounceable address (for init): {}\nBounceable address (for later access): {}\nraw: {}",
                nonBounceableAddress,
                bounceableAddress,
                address.toString(false));

        if (StringUtils.isEmpty(predefinedSecretKey)) {
            BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(50));
            log.info("new wallet balance {}", Utils.formatNanoValue(balance));
            // deploy new wallet
            tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));
        }

        long seqno = wallet.getSeqno(tonlib);
        log.info("wallet seqno {}", seqno);
    }

    @Test
    public void testJetton() {

        JettonMinter minter = delployMinter();
        Utils.sleep(15);
        getMinterInfo(minter);
        mint(minter);

        log.info("jettonWalletAddress {}", JETTON_WALLET_ADDRESS);

        JettonWallet jettonWallet = getJettonWalletInfo();

        editContent(minter);
        changeAdmin(minter);

        transfer(jettonWallet);
        burn(jettonWallet);

    }

    private JettonMinter delployMinter() {
        Options options = Options.builder()
                .adminAddress(wallet.getAddress())
                .jettonContentUri("https://ton.org/jetton.json")
                .jettonWalletCodeHex(JettonWallet.JETTON_WALLET_CODE_HEX)
                .wc(0L)
                .build();

        Wallet jettonMinter = new Wallet(WalletVersion.jettonMinter, options);
        JettonMinter minter = jettonMinter.create();
        log.info("jetton minter address {}", minter.getAddress().toString(true, true, true));
// EQCWdvNy5xvwdhJYVRhEoN_QDUAMsZgSDL-Ga9Y_Wo9b-0dn

        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                minter.getAddress(),
                Utils.toNano(0.05),
                seqno,
                (Cell) null, // body
                (byte) 3, //send mode
                false, //dummy signature
                minter.createStateInit().stateInit
        );

        String base64bocExtMsg = Utils.bytesToBase64(extMsg.message.toBoc(false));
        tonlib.sendRawMessage(base64bocExtMsg);
        return minter;
    }

    private JettonWallet getJettonWalletInfo() {
        Options optionsJettonWallet = Options.builder()
                .address(Address.of(JETTON_WALLET_ADDRESS))
                .build();

        Wallet jettonW = new Wallet(WalletVersion.jettonWallet, optionsJettonWallet);
        JettonWallet jettonWallet = jettonW.create();

        JettonWalletData data = jettonWallet.getData(tonlib);
        log.info("jettonWalletData {}", data);
        return jettonWallet;
    }

    private void getMinterInfo(JettonMinter minter) {
        JettonMinterData data = minter.getJettonData(tonlib);
        log.info("JettonMinterData {}", data);
        log.info("JettonMinterData adminAddress {}", data.getAdminAddress().toString(true, true, true));
//        log.info("JettonMinterData contentCell {}", data.getJettonContentCell().toHex());
//        log.info("JettonMinterData jettonWalletCode {}", data.getJettonWalletCode().toHex());

        Address jettonWalletAddress = minter.getJettonWalletAddress(tonlib, wallet.getAddress());
        log.info("getJettonWalletAddress {}", jettonWalletAddress.toString(true, true, true));
        log.info("getJettonWalletAddress {}", jettonWalletAddress);
    }

    private void mint(JettonMinter minter) {
        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                minter.getAddress(),
                Utils.toNano(0.05),
                seqno,
                minter.createMintBody(0, wallet.getAddress(), Utils.toNano(0.04), Utils.toNano(100500)),
                (byte) 3, //send mode
                false, //dummy signature
                null
        );


        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }

    private void editContent(JettonMinter minter) {
        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                minter.getAddress(),
                Utils.toNano(0.05),
                seqno,
                minter.createEditContentBody("http://localhost/nft-marketplace/my_collection.123", 0),
                (byte) 3, //send mode
                false, //dummy signature
                null
        );

        String base64bocExtMsg = Utils.bytesToBase64(extMsg.message.toBoc(false));
        tonlib.sendRawMessage(base64bocExtMsg);
    }

    private void changeAdmin(JettonMinter minter) {
        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                minter.getAddress(),
                Utils.toNano(0.05),
                seqno,
                minter.createChangeAdminBody(0, Address.of(WALLET2_ADDRESS)),
                (byte) 3, //send mode
                false, //dummy signature
                null
        );

        String base64bocExtMsg = Utils.bytesToBase64(extMsg.message.toBoc(false));
        tonlib.sendRawMessage(base64bocExtMsg);
    }

    private void transfer(JettonWallet jettonWallet) {

        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                Address.of(JETTON_WALLET_ADDRESS),
                Utils.toNano(0.05),
                seqno,
                jettonWallet.createTransferBody(0,
                        Utils.toNano(500),
                        Address.of(WALLET2_ADDRESS),
                        wallet.getAddress(),
                        Utils.toNano("0.01"),
                        "gift".getBytes()
                ),
                (byte) 3, //send mode
                false, //dummy signature
                null
        );

        String base64bocExtMsg = Utils.bytesToBase64(extMsg.message.toBoc(false));
        tonlib.sendRawMessage(base64bocExtMsg);
    }

    private void burn(JettonWallet jettonWallet) {

        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                Address.of(JETTON_WALLET_ADDRESS),
                Utils.toNano(0.05),
                seqno,
                jettonWallet.createBurnBody(0,
                        Utils.toNano(400),
                        wallet.getAddress()
                ),
                (byte) 3, //send mode
                false, //dummy signature
                null
        );

        String base64bocExtMsg = Utils.bytesToBase64(extMsg.message.toBoc(false));
        tonlib.sendRawMessage(base64bocExtMsg);
    }
}
