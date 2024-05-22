package org.ton.java.smartcontract.integrationtests;

import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.GenerateWallet;
import org.ton.java.smartcontract.token.ft.JettonMinterV2;
import org.ton.java.smartcontract.token.ft.JettonWalletV2;
import org.ton.java.smartcontract.types.JettonMinterData;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.wallet.v3.WalletV3R1;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestJettonV2 {
    public static final String NEW_ADMIN2 = "EQB6-6po0yspb68p7RRetC-hONAz-JwxG9514IEOKw_llXd5";

    static WalletV3R1 adminWallet;
    static WalletV3R1 wallet2;
    static Tonlib tonlib;

    @BeforeClass
    public static void setUpBeforeClass() throws InterruptedException {
        tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .build();
        adminWallet = GenerateWallet.random(tonlib, 7);
        wallet2 = GenerateWallet.random(tonlib, 1);
    }

    @Test
    public void testJettonMinterV2() {
        JettonMinterV2 minter = JettonMinterV2.builder()
                .tonlib(tonlib)
                .adminAddress(adminWallet.getAddress())
                .nextAdminAddress(Address.of(NEW_ADMIN2))
                .jettonContentUri("https://raw.githubusercontent.com/neodix42/ton4j/main/1-media/neo-jetton.json")
                .jettonWalletCodeHex(WalletCodes.jettonWalletV2.getValue())
                .build();

        log.info("jetton minter address {}", minter.getAddress());

        // DEPLOY MINTER V2

//        WalletV3Config walletV3Config = WalletV3Config.builder()
//                .walletId(42)
//                .seqno(adminWallet.getSeqno())
//                .destination(minter.getAddress())
//                .amount(Utils.toNano(1.1))
//                .stateInit(minter.getStateInit())
//                .comment("deploy minter") // todo
//                .build();
//
//        ExtMessageInfo extMessageInfo = adminWallet.sendTonCoins(walletV3Config);
//        assertThat(extMessageInfo.getError().getCode()).isZero();
//        log.info("deploying minter V2");
//        minter.waitForDeployment(60);
//
//        getMinterInfoV2(minter); // nothing minted, so zero returned

        // DEPLOY MINTER AND MINT JETTONS

        WalletV3Config walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(minter.getAddress())
                .amount(Utils.toNano(2))
                .stateInit(minter.getStateInit())
                .body(JettonMinterV2.createMintBody(0,
                        adminWallet.getAddress(),
                        Utils.toNano(2), // ton amount
                        Utils.toNano(100500), // jetton amount
                        null, // from address
                        null, // response address
                        BigInteger.ZERO)) // fwd amount
                .comment("mint tokens")
                .build();

        ExtMessageInfo extMessageInfo = adminWallet.sendTonCoins(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(45, "minting...");

        getMinterInfoV2(minter);


        // transfer from admin to wallet2 by sending transfer request to admin's jetton wallet
        JettonWalletV2 adminJettonWallet = minter.getJettonWallet(adminWallet.getAddress());

        walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(adminJettonWallet.getAddress())
                .amount(Utils.toNano(0.057))
                .body(JettonWalletV2.createTransferBody(
                                0,
                                Utils.toNano(444),
                                wallet2.getAddress(), // recipient
                                adminWallet.getAddress(), // response address
                                Utils.toNano("0.01"), // forward amount
                                "gift".getBytes() // forward payload
                        )
                )
                .build();
        extMessageInfo = adminWallet.sendTonCoins(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "transferring 444 jettons...");
        log.info("admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance()));

        getMinterInfoV2(minter);

        // LOCK JETTONS in ADMIN WALLET

        walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(adminJettonWallet.getAddress())
                .amount(Utils.toNano(0.05))
                .body(JettonWalletV2.createStatusBody(
                        0,
                        3))// cant send and receive
                .build();
        extMessageInfo = adminWallet.sendTonCoins(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "locking jettons in admin wallet");
        log.info("admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance()));

    }

    private void getMinterInfoV2(JettonMinterV2 minter) {
        JettonMinterData data = minter.getJettonData(tonlib);
        log.info("minter adminAddress {}", data.getAdminAddress());
        log.info("minter totalSupply {}", Utils.formatNanoValue(data.getTotalSupply()));
        log.info("minter jetton uri {}", data.getJettonContentUri());
    }
}
