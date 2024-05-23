package org.ton.java.smartcontract.integrationtests;

import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.GenerateWallet;
import org.ton.java.smartcontract.token.ft.JettonMinter;
import org.ton.java.smartcontract.token.ft.JettonWallet;
import org.ton.java.smartcontract.types.JettonMinterData;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.v3.WalletV3R1;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestJetton {
    public static final String NEW_ADMIN2 = "EQB6-6po0yspb68p7RRetC-hONAz-JwxG9514IEOKw_llXd5";

    static WalletV3R1 adminWallet;
    static WalletV3R1 wallet2;
    static Tonlib tonlib;

    @BeforeClass
    public static void setUpBeforeClass() throws InterruptedException {
        tonlib = Tonlib.builder().testnet(true).ignoreCache(false).build();
        adminWallet = GenerateWallet.random(tonlib, 2);
        wallet2 = GenerateWallet.random(tonlib, 1);
    }

    @Test
    public void testJettonMinter() throws InterruptedException {

        log.info("admin wallet address {}", adminWallet.getAddress());
        log.info("second wallet address {}", wallet2.getAddress());

        JettonMinter minter = JettonMinter.builder()
                .tonlib(tonlib)
                .adminAddress(adminWallet.getAddress())
                .jettonContentUri("https://raw.githubusercontent.com/neodix42/ton4j/main/1-media/neo-jetton.json")
                .jettonWalletCodeHex(WalletCodes.jettonWallet.getValue())
                .build();

        log.info("jetton minter address {}", minter.getAddress());

        // DEPLOY MINTER

        WalletV3Config walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(minter.getAddress())
                .amount(Utils.toNano(0.2))
                .stateInit(minter.getStateInit())
                .comment("deploy minter")
                .build();

        ExtMessageInfo extMessageInfo = adminWallet.sendTonCoins(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        log.info("deploying minter");
        minter.waitForDeployment(60);

        getMinterInfo(minter); // nothing minted, so zero returned

        // MINT JETTONS

        walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(minter.getAddress())
                .amount(Utils.toNano(0.07))
                .body(minter.createMintBody(0,
                        adminWallet.getAddress(),
                        Utils.toNano(0.07),
                        Utils.toNano(100500),
                        null,
                        null,
                        BigInteger.ONE,
                        MsgUtils.createTextMessageBody("minting"))
                ).build();

        extMessageInfo = adminWallet.sendTonCoins(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(45, "minting...");

        getMinterInfo(minter);

        // EDIT MINTER'S JETTON CONTENT

        walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(minter.getAddress())
                .amount(Utils.toNano(0.055))
                .body(minter.createEditContentBody("http://localhost/nft-marketplace/my_collection_1.json", 0))
                .build();
        extMessageInfo = adminWallet.sendTonCoins(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "edit minter content, OP 4");

        getMinterInfo(minter);

        // CHANGE MINTER ADMIN
        log.info("newAdmin {}", Address.of(NEW_ADMIN2));

        walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(minter.getAddress())
                .amount(Utils.toNano(0.056))
                .body(minter.createChangeAdminBody(0, Address.of(NEW_ADMIN2)))
                .build();
        extMessageInfo = adminWallet.sendTonCoins(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "change minter admin, OP 3");

        getMinterInfo(minter);

        Utils.sleep(30);

        log.info("adminWallet balance: {}", Utils.formatNanoValue(adminWallet.getBalance()));
        log.info("    wallet2 balance: {}", Utils.formatNanoValue(wallet2.getBalance()));


        JettonWallet adminJettonWallet = minter.getJettonWallet(adminWallet.getAddress());

        // transfer from admin to WALLET2_ADDRESS by sending transfer request to admin's jetton wallet

        walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(adminJettonWallet.getAddress())
                .amount(Utils.toNano(0.057))
                .body(JettonWallet.createTransferBody(
                                0,
                                Utils.toNano(444),
                                wallet2.getAddress(),         // recipient
                                adminWallet.getAddress(),     // response address
                                BigInteger.ONE, // forward amount
                                MsgUtils.createTextMessageBody("gift") // forward payload
                        )
                )
                .build();
        extMessageInfo = adminWallet.sendTonCoins(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "transferring 444 jettons...");
        log.info("admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance()));

        //wallet 2, after received jettons, can use JettonWallet
        JettonWallet jettonWallet2 = minter.getJettonWallet(wallet2.getAddress());
        log.info("wallet2 balance {}", Utils.formatNanoValue(jettonWallet2.getBalance()));

        // BURN JETTONS in ADMIN WALLET

        walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(adminJettonWallet.getAddress())
                .amount(Utils.toNano(0.05))
                .body(JettonWallet.createBurnBody(
                        0,
                        Utils.toNano(111),
                        adminWallet.getAddress())
                )
                .build();
        extMessageInfo = adminWallet.sendTonCoins(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "burning 111 jettons in admin wallet");
        log.info("admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance()));

        getMinterInfo(minter);
    }


    private void getMinterInfo(JettonMinter minter) {
        JettonMinterData data = minter.getJettonData(tonlib);
        log.info("minter adminAddress {}", data.getAdminAddress());
        log.info("minter totalSupply {}", Utils.formatNanoValue(data.getTotalSupply()));
        log.info("minter jetton uri {}", data.getJettonContentUri());
    }
}
