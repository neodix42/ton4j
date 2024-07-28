package org.ton.java.smartcontract.integrationtests;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.GenerateWallet;
import org.ton.java.smartcontract.highload.HighloadWalletV3;
import org.ton.java.smartcontract.token.ft.JettonMinter;
import org.ton.java.smartcontract.token.ft.JettonWallet;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.*;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.v3.WalletV3R1;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestJetton {
    public static final String NEW_ADMIN2 = "EQB6-6po0yspb68p7RRetC-hONAz-JwxG9514IEOKw_llXd5";

    static WalletV3R1 adminWallet;
    static WalletV3R1 wallet2;
    static HighloadWalletV3 highloadWallet2;
    static Tonlib tonlib;

    @Test
    public void testJettonMinter() throws InterruptedException {

        tonlib = Tonlib.builder().testnet(true).ignoreCache(false).build();
        adminWallet = GenerateWallet.randomV3R1(tonlib, 2);
        wallet2 = GenerateWallet.randomV3R1(tonlib, 1);

        log.info("admin wallet address {}", adminWallet.getAddress());
        log.info("second wallet address {}", wallet2.getAddress());

        JettonMinter minter = JettonMinter.builder()
                .tonlib(tonlib)
                .adminAddress(adminWallet.getAddress())
                .content(NftUtils.createOffChainUriCell("https://raw.githubusercontent.com/neodix42/ton4j/main/1-media/neo-jetton.json"))
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

        ExtMessageInfo extMessageInfo = adminWallet.send(walletV3Config);
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
                .body(JettonMinter.createMintBody(0,
                        adminWallet.getAddress(),
                        Utils.toNano(0.07),
                        Utils.toNano(100500),
                        null,
                        null,
                        BigInteger.ONE,
                        MsgUtils.createTextMessageBody("minting"))
                ).build();

        extMessageInfo = adminWallet.send(walletV3Config);
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
        extMessageInfo = adminWallet.send(walletV3Config);
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
        extMessageInfo = adminWallet.send(walletV3Config);
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
                                null, // custom payload
                                BigInteger.ONE, // forward amount
                                MsgUtils.createTextMessageBody("gift") // forward payload
                        )
                )
                .build();
        extMessageInfo = adminWallet.send(walletV3Config);
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
        extMessageInfo = adminWallet.send(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "burning 111 jettons in admin wallet");
        log.info("admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance()));

        getMinterInfo(minter);
    }


    @Test
    public void testJettonTransferWithHighloadWalletV3_800() throws InterruptedException, NoSuchAlgorithmException {

        tonlib = Tonlib.builder().testnet(true).ignoreCache(false).build();

        adminWallet = GenerateWallet.randomV3R1(tonlib, 2);
        highloadWallet2 = GenerateWallet.randomHighloadV3R1(tonlib, 55);

        // DEPLOY MINTER AND MINT JETTONS

        JettonMinter minter = JettonMinter.builder()
                .tonlib(tonlib)
                .adminAddress(adminWallet.getAddress())
                .content(NftUtils.createOffChainUriCell("https://raw.githubusercontent.com/neodix42/ton4j/main/1-media/neo-jetton.json"))
                .jettonWalletCodeHex(WalletCodes.jettonWallet.getValue())
                .build();

        log.info("jetton minter address {}", minter.getAddress());

        WalletV3Config walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(minter.getAddress())
                .amount(Utils.toNano(1.2))
                .stateInit(minter.getStateInit())
                .body(JettonMinter.createMintBody(0,
                                adminWallet.getAddress(),
                                Utils.toNano(0.1), // ton amount
                                BigInteger.valueOf(100500_00), // jetton amount
                                null, // from address
                                null, // response address
                                BigInteger.ONE, // fwd amount
                                MsgUtils.createTextMessageBody("minting") // forward payload
                        )
                )
                .build();
        ExtMessageInfo extMessageInfo = adminWallet.send(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        log.info("deploying minter and minting...");
        minter.waitForDeployment(60);
        Utils.sleep(10);

        getMinterInfo(minter);

        log.info("adminWallet balance: {}", Utils.formatNanoValue(adminWallet.getBalance()));

        // TRANSFER from adminWallet to highloadWallet2 (by sending transfer request to admin's jettonWallet)
        JettonWallet adminJettonWallet = minter.getJettonWallet(adminWallet.getAddress());

        log.info("adminJettonWallet balance: {}, address: {}", Utils.formatNanoValue(adminJettonWallet.getBalance(), 2), adminJettonWallet.getAddress());

        walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(adminJettonWallet.getAddress())
                .amount(Utils.toNano(0.057))
                .body(JettonWallet.createTransferBody(
                                0,
                                BigInteger.valueOf(2000_00), // 2 decimals
                                highloadWallet2.getAddress(), // recipient
                                null, // response address
                                null, // custom payload
                                BigInteger.ONE, // forward amount
                                MsgUtils.createTextMessageBody("gift") // forward payload
                        )
                )
                .build();
        extMessageInfo = adminWallet.send(walletV3Config);
        Assertions.assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "transferring 2000 jettons from adminWallet to highloadWallet2...");


        JettonWallet highloadJettonWallet2 = minter.getJettonWallet(highloadWallet2.getAddress());

        // transfer jettons from highloadWallet2 to two destinations by sending transfer request to highload's jetton wallet

        HighloadV3Config highloadV3Config = HighloadV3Config.builder()
                .walletId(42)
                .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
                .body(
                        highloadWallet2.createBulkTransfer(
                                createDummyDestinations(800, highloadJettonWallet2),
                                BigInteger.valueOf(HighloadQueryId.fromSeqno(1).getQueryId()))
                )
                .build();
        extMessageInfo = highloadWallet2.send(highloadV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "transferring to 800 recipients 2 jettons...");
        log.info("admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance()));

        getMinterInfo(minter);
    }


    private void getMinterInfo(JettonMinter minter) {
        JettonMinterData data = minter.getJettonData(tonlib);
        log.info("minter adminAddress {}", data.getAdminAddress());
        log.info("minter totalSupply {}", Utils.formatNanoValue(data.getTotalSupply()));
        log.info("minter jetton uri {}", data.getJettonContentUri());
    }


    List<Destination> createDummyDestinations(int count, JettonWallet jettonWallet) throws NoSuchAlgorithmException {
        List<Destination> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String dstDummyAddress = "0:" + Utils.bytesToHex(MessageDigest.getInstance("SHA-256").digest(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));

            result.add(Destination.builder()
                    .bounce(false)
                    .address(jettonWallet.getAddress().toBounceable())
                    .amount(Utils.toNano(0.06))
                    .body(JettonWallet.createTransferBody(
                            0,
                            BigInteger.valueOf(2_00), // 2 jettons, with decimals = 2
                            Address.of(dstDummyAddress),         // recipient
                            null,     // response address
                            null,     // custom payload
                            BigInteger.ONE, // forward amount
                            MsgUtils.createTextMessageBody("test sdk") // forward payload / memo
                    ))
                    .build());
        }
        return result;
    }
}
