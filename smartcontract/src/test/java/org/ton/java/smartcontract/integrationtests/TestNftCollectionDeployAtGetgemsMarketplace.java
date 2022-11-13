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
import org.ton.java.smartcontract.token.nft.NftCollection;
import org.ton.java.smartcontract.token.nft.NftItem;
import org.ton.java.smartcontract.token.nft.NftMarketplace;
import org.ton.java.smartcontract.token.nft.NftSale;
import org.ton.java.smartcontract.types.*;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.smartcontract.wallet.v3.WalletV3ContractR1;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

// await deployNftCollection();
// await getNftCollectionInfo();
// await deployNftItem();
// await getNftItemInfo();
// await getSingleNftItemInfo();
// await deployMarketplace();
// await deploySale();
// await getSaleInfo();
// await getStaticData();
// await transferNftItem();
// await cancelSale();
// await changeCollectionOwner();
// await editCollectionContent();
// await getRoyaltyParams();

@Slf4j
@RunWith(JUnit4.class)
public class TestNftCollectionDeployAtGetgemsMarketplace {

    private static final String TESTNET_GETGEMS_NFT_MARKETPLACE_ADDRESS = "EQBZp2tZ9WUZQP8AgL2gUHkdJQe-8NyAcFksn3L7dcZxYCKH";
    private static final String WALLET2_ADDRESS = "EQB6-6po0yspb68p7RRetC-hONAz-JwxG9514IEOKw_llXd5";

    private Address nftItem1;
    private Address nftItem2;
    static TweetNaclFast.Signature.KeyPair adminKeyPair1;
    static WalletV3ContractR1 adminWallet;
    static Tonlib tonlib = Tonlib.builder()
            .testnet(true)
//            .verbosityLevel(VerbosityLevel.DEBUG)
            .build();

    @BeforeClass
    public static void setUpClass() throws InterruptedException {
//        String predefinedSecretKey = "955e795bb5ae3f912080c6223a272a308db44dc8643750a91b99c3a557feabe25aa151b2072004dc9e7c4d751bf19e8a0b0a09fdadaa816ad848dac7256bf28e";
        String predefinedSecretKey = "";
//        kQD54CgV4MSeKTD1DYatJt_AZwW5sb0loN0rEy5VojbU-upC
//        0:f9e02815e0c49e2930f50d86ad26dfc06705b9b1bd25a0dd2b132e55a236d4fa

        if (StringUtils.isEmpty(predefinedSecretKey)) {
            adminKeyPair1 = Utils.generateSignatureKeyPair();
        } else {
            adminKeyPair1 = Utils.generateSignatureKeyPairFromSeed(Utils.hexToBytes(predefinedSecretKey));
        }

        log.info("pubKey {}, prvKey {}", Utils.bytesToHex(adminKeyPair1.getPublicKey()), Utils.bytesToHex(adminKeyPair1.getSecretKey()));

        Options options = Options.builder()
                .publicKey(adminKeyPair1.getPublicKey())
                .wc(0)
                .build();

        Wallet walletcontract = new Wallet(WalletVersion.v3R1, options);
        adminWallet = walletcontract.create();

        InitExternalMessage msg = adminWallet.createInitExternalMessage(adminKeyPair1.getSecretKey());
        Address address = msg.address;

        String nonBounceableAddress = address.toString(true, true, false, true);
        String bounceableAddress = address.toString(true, true, true, true);

        log.info("\nNon-bounceable address (for init): {}\nBounceable address (for later access): {}\nraw: {}", nonBounceableAddress, bounceableAddress, address.toString(false));

        if (StringUtils.isEmpty(predefinedSecretKey)) {
            BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
            log.info("new wallet balance {}", Utils.formatNanoValue(balance));

            // deploy new wallet
            tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));
            Utils.sleep(15);
        }
    }

    @Test
    public void testCreateAndDeployNftCollection() {

        log.info("admin wallet address {}", adminWallet.getAddress().toString(true, true, true));

//        editNftCollectionContent(tonlib, adminWallet, Utils.toNano(0.055),
//                Address.of("EQCfjT1nWNdOI-6OyCGVVKZLkWmbbS1U4ibMgPVPtRORkd2R"),
//                "https://raw.githubusercontent.com/neodiX42/ton4j/dns-smc/1-media/nft-collection.json",
//                "https://raw.githubusercontent.com/neodiX42/ton4j/dns-smc/1-media/",
//                0.16,
//                adminWallet.getAddress(),
//                adminKeyPair1);

        Options optionsNftCollection = Options.builder()
                .adminAddress(adminWallet.getAddress())
                .royalty(0.13)
                .royaltyAddress(adminWallet.getAddress())
                .collectionContentUri("https://raw.githubusercontent.com/neodiX42/ton4j/dns-smc/1-media/nft-collection.json")
                .collectionContentBaseUri("https://raw.githubusercontent.com/neodiX42/ton4j/dns-smc/1-media/")
                .nftItemCodeHex(NftItem.NFT_ITEM_CODE_HEX)
                .build();

        Wallet nftCollectionWallet = new Wallet(WalletVersion.nftCollection, optionsNftCollection);
        NftCollection nftCollection = nftCollectionWallet.create();
        log.info("NFT collection address {}", nftCollection.getAddress().toString(true, true, true));

        nftCollection.deploy(tonlib, adminWallet, Utils.toNano(1), adminKeyPair1);

        Utils.sleep(15, "deploying NFT collection");
        getNftCollectionInfo(nftCollection);

        // create and deploy NFT Item
        deployNftItem(tonlib, adminWallet, BigInteger.ZERO, Utils.toNano(0.06), nftCollection.getAddress(), "nft-item-1.json", adminKeyPair1);
        Utils.sleep(15, "deploying NFT item #1");

        deployNftItem(tonlib, adminWallet, BigInteger.ONE, Utils.toNano(0.06), nftCollection.getAddress(), "nft-item-2.json", adminKeyPair1);
        Utils.sleep(25, "deploying NFT item #2");

        getNftCollectionInfo(nftCollection);

        NftItem item1 = new NftItem(Options.builder().address(nftItem1).build());
        NftItem item2 = new NftItem(Options.builder().address(nftItem2).build());
//        getNftItemInfo(nftCollection, item1);
//        getNftItemInfo(nftCollection, item2);


        // get getgems test-nft-marketplace address
        NftMarketplace marketplace = new NftMarketplace(Options.builder().address(Address.of(TESTNET_GETGEMS_NFT_MARKETPLACE_ADDRESS)).build());
        log.info("getgems testnet nft marketplace address {}", marketplace.getAddress().toString(true, true, true));
        // getgems protected their nft marketplace, so we cannot place our nft-sale on it

        // instead of nft-marketplace address we can specify any other your wallet
        // set the price, royalty and fee for your nft item
        log.info("wallet2 {}", WALLET2_ADDRESS);
        Options optionsNftSale = Options.builder()
//                .marketplaceAddress(marketplace.getAddress())
                .marketplaceAddress(adminWallet.getAddress())
                .nftItemAddress(Address.of(nftItem1))
                .fullPrice(Utils.toNano(1.1))
                .marketplaceFee(Utils.toNano(0.4))
                .royaltyAddress(nftCollection.getAddress())
                .royaltyAmount(Utils.toNano(0.3))
                .build();

        NftSale nftSale = new NftSale(optionsNftSale);
        log.info("nft sale address {}", nftSale.getAddress().toString(true, true, true));

        nftSale.deploy(tonlib, adminWallet, Utils.toNano(0.6), adminWallet.getAddress(), adminKeyPair1);
        Utils.sleep(15, "deploying NFT sale smart-contract for nft item #1");

        // get nft item data
        NftSaleData data = nftSale.getData(tonlib);
        log.info("nftSale data for nft item #1 {}", data);

        // transfer nft item to nft-sale smart-contract (send amount > full_price+1ton)
        transferNftItem(tonlib, adminWallet, Utils.toNano(1.5), item1, nftSale.getAddress(), Utils.toNano(0.02), "gift".getBytes(), adminWallet.getAddress(), adminKeyPair1);


        // changed my mind, remove nft-sale smc
        nftSale.cancel(tonlib, adminWallet, Utils.toNano(1), nftSale.getAddress(), 0, adminKeyPair1);

        //after changed owner this will fail with 401 error
        editNftCollectionContent(tonlib, adminWallet, Utils.toNano(0.055), nftCollection.getAddress(), "https://raw.githubusercontent.com/neodiX42/ton4j/dns-smc/1-media/nft-collection.json", "ton://my-nft/", 0.16, Address.of(WALLET2_ADDRESS), adminKeyPair1);

        //tested ok!
        changeNftCollectionOwner(tonlib, adminWallet, Utils.toNano(0.06), nftCollection.getAddress(), Address.of(WALLET2_ADDRESS), adminKeyPair1);
    }

    private void getNftCollectionInfo(NftCollection nftCollection) {
        CollectionData data = nftCollection.getCollectionData(tonlib);
        log.info("nft collection info {}", data);
        log.info("nft collection item count {}", data.getNextItemIndex());
        log.info("nft collection owner {}", data.getOwnerAddress());

        nftItem1 = nftCollection.getNftItemAddressByIndex(tonlib, BigInteger.ZERO);
        nftItem2 = nftCollection.getNftItemAddressByIndex(tonlib, BigInteger.ONE);

        log.info("address at index 1 = {}", nftItem1.toString(true, true, true));
        log.info("address at index 2 = {}", nftItem2.toString(true, true, true));

        Royalty royalty = nftCollection.getRoyaltyParams(tonlib);
        Address royaltyAddress = royalty.getRoyaltyAddress();
        log.info("nft collection royalty address {}", royaltyAddress.toString(true, true, true));
    }

    public void deployNftItem(Tonlib tonlib, WalletContract wallet, BigInteger index, BigInteger msgValue, Address nftCollectionAddress, String nftItemContentUri, TweetNaclFast.Signature.KeyPair keyPair) {

        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                nftCollectionAddress,
                msgValue,
                seqno,
                NftCollection.createMintBody(
                        0,
                        index,
                        msgValue,
                        wallet.getAddress(),
                        nftItemContentUri));

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }

    public void changeNftCollectionOwner(Tonlib tonlib, WalletContract wallet, BigInteger msgValue, Address nftCollectionAddress, Address newOwner, TweetNaclFast.Signature.KeyPair keyPair) {

        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                nftCollectionAddress,
                msgValue,
                seqno,
                NftCollection.createChangeOwnerBody(0, newOwner)
        );

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }

    public void editNftCollectionContent(Tonlib tonlib, WalletContract wallet, BigInteger msgValue, Address nftCollectionAddress, String collectionContentUri, String nftItemContentBaseUri, double royalty, Address royaltyAddress, TweetNaclFast.Signature.KeyPair keyPair) {

        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                nftCollectionAddress,
                msgValue,
                seqno,
                NftCollection.createEditContentBody(
                        0,
                        collectionContentUri,
                        nftItemContentBaseUri,
                        royalty,
                        royaltyAddress)
        );

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }

    public void getRoyaltyParams(Tonlib tonlib, WalletContract wallet, BigInteger msgValue, Address nftCollectionAddress, TweetNaclFast.Signature.KeyPair keyPair) {

        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                nftCollectionAddress,
                msgValue,
                seqno,
                NftCollection.createGetRoyaltyParamsBody(0)
        );

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }

    private void getNftItemInfo(NftCollection nftCollection, NftItem nftItem) {
        ItemData data = nftCollection.getNftItemContent(tonlib, nftItem);
        log.info("nftItem {}", data);
    }

    private void getSingleNftItemInfo(NftItem nftItem) {
        ItemData data = nftItem.getData(tonlib);
        log.info("nftItem {}", data);
        Royalty royalty = nftItem.getRoyaltyParams(tonlib);
        log.info("nftItem royalty {}", royalty);
    }

    private void transferNftItem(Tonlib tonlib, WalletContract wallet, BigInteger msgValue, NftItem nftItem, Address nftSaleAddress, BigInteger forwardAmount, byte[] forwardPayload, Address responseAddress, TweetNaclFast.Signature.KeyPair keyPair) {

        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                nftItem.getAddress(),
                msgValue,
                seqno,
                NftItem.createTransferBody(
                        0,
                        nftSaleAddress, //new owner
                        forwardAmount,
                        forwardPayload,
                        responseAddress)
        );

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }

    private void getStaticData(Tonlib tonlib, WalletContract wallet, BigInteger msgValue, Address nftItemAddress, long queryId, TweetNaclFast.Signature.KeyPair keyPair) {
        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                nftItemAddress,
                msgValue,
                seqno,
                NftItem.createGetStaticDataBody(queryId)
        );

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }

    private void deploySale(Tonlib tonlib, WalletContract wallet, BigInteger msgValue, Address nftItemAddress, long queryId, TweetNaclFast.Signature.KeyPair keyPair) {
        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                nftItemAddress,
                msgValue,
                seqno,
                NftItem.createGetStaticDataBody(queryId)
        );

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }

}
