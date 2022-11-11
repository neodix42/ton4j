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
import org.ton.java.smartcontract.nft.NftCollection;
import org.ton.java.smartcontract.nft.NftItem;
import org.ton.java.smartcontract.nft.NftMarketplace;
import org.ton.java.smartcontract.nft.NftSale;
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
        String predefinedSecretKey = "f437d871b3ca5f110d49662f5d702f85ff36c282c22c201e9f4ee7e7b01dc41f759fe2cd7dadfa0aa420b831a0374c2462201f8af379671bc9a75a2fc209aea9";
//        String predefinedSecretKey = "";
//        EQA7qttYjKB46KonCtvTFnj0uv4LySr6jg6qHwYHC5ukH-Hg
//        0:3baadb588ca078e8aa270adbd31678f4bafe0bc92afa8e0eaa1f06070b9ba41f

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
            BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(15));
            log.info("new wallet balance {}", Utils.formatNanoValue(balance));
            Utils.sleep(10);
            // deploy new wallet
            tonlib.sendRawMessage(Utils.bytesToBase64(msg.message.toBoc(false)));
        }

        long seqno = adminWallet.getSeqno(tonlib);
        log.info("wallet seqno {}", seqno);
    }

    @Test
    public void testCreateAndDeployNftCollection() {

        log.info("admin wallet address {}", adminWallet.getAddress().toString(true, true, true));

        Options optionsNftCollection = Options.builder()
                .adminAddress(adminWallet.getAddress())
                .royalty(0.13)
                .royaltyAddress(adminWallet.getAddress())
                .collectionContentUri("https://github.com/neodiX42/ton4j/blob/dns-smc/1-media/nft-collection.json")
                .collectionContentBaseUri("https://github.com/neodiX42/ton4j/blob/dns-smc/1-media/")
                .nftItemCodeHex(NftItem.NFT_ITEM_CODE_HEX)
                .build();

        Wallet nftCollectionWallet = new Wallet(WalletVersion.nftCollection, optionsNftCollection);
        NftCollection nftCollection = nftCollectionWallet.create();
        log.info("NFT collection address {}", nftCollection.getAddress().toString(true, true, true));

        nftCollection.deploy(tonlib, adminWallet, Utils.toNano(1), adminKeyPair1);

        Utils.sleep(15);
        getNftCollectionInfo(nftCollection);
        getRoyaltyParams(tonlib, adminWallet, Utils.toNano(0.0777), nftCollection.getAddress(), adminKeyPair1);

        // create and deploy NFT Item
        deployNftItem(tonlib, adminWallet, Utils.toNano(0.06), nftCollection.getAddress(),
                "https://github.com/neodiX42/ton4j/blob/dns-smc/1-media/nft-item-1.json", adminKeyPair1);
        Utils.sleep(15);

        // get nft info -- fails
        NftItem nftItem = new NftItem(Options.builder().address(nftItem1).build());
//        getNftItemInfo(nftCollection, nftItem);
//        getSingleNftItemInfo(new NftItem(Options.builder().address(nftItem2).build()));

        // get getgems test nft marketplace
        NftMarketplace marketplace = new NftMarketplace(Options.builder().adminAddress(Address.of(TESTNET_GETGEMS_NFT_MARKETPLACE_ADDRESS)).build());
        log.info("getgems testnet nft marketplace address {}", marketplace.getAddress().toString(true, true, true));

        //deploy nft sale smart-contract, so we could sell our nft collection on marketplace
        Options optionsNftSale = Options.builder()
                .marketplaceAddress(marketplace.getAddress())
                .nftItemAddress(Address.of(nftItem1))
                .fullPrice(Utils.toNano(1.1))
                .marketplaceFee(Utils.toNano(0.4))
                .royaltyAddress(nftCollection.getAddress())
                .royaltyAmount(Utils.toNano(0.3))
                .build();

        NftSale nftSale = new NftSale(optionsNftSale);
        log.info("nft sale address {}", nftSale.getAddress().toString(true, true, true));
        nftSale.deploy(tonlib, adminWallet, Utils.toNano(0.06), marketplace.getAddress(), adminKeyPair1);
        Utils.sleep(15);
        //our nft item is now visible on testnet getgems nft marketplace

        // get nft item data
        NftSaleData data = nftSale.getData(tonlib);
        log.info("nftSale data {}", data);

        // transfer nft item to nft sale smart-contract (send amount > full_price+1ton)
        transferNftItem(tonlib, adminWallet, Utils.toNano(1.5), nftItem, nftSale.getAddress(), Utils.toNano(0.02), "gift".getBytes(), adminWallet.getAddress(), adminKeyPair1);

        // removes nft-sale smc?
        nftSale.cancel(tonlib, adminWallet, Utils.toNano(1), nftSale.getAddress(), 0, adminKeyPair1);

        //after changed owner this will fail with 401 error
        editNftCollectionContent(tonlib, adminWallet, Utils.toNano(0.055), nftCollection.getAddress(), "ton://my-nft/collection.json", "ton://my-nft/", 0.16, Address.of(WALLET2_ADDRESS), adminKeyPair1);

        //tested ok!
        changeNftCollectionOwner(tonlib, adminWallet, Utils.toNano(0.06), nftCollection.getAddress(), Address.of(WALLET2_ADDRESS), adminKeyPair1);
    }

    private void getNftCollectionInfo(NftCollection nftCollection) {
        CollectionData data = nftCollection.getCollectionData(tonlib);
        log.info("nft collection info {}", data);
        log.info("nft collection item count {}", data.getItemsCount());
        log.info("nft collection owner {}", data.getOwnerAddress());

        nftItem1 = nftCollection.getNftItemAddressByIndex(tonlib, BigInteger.ZERO);
        nftItem2 = nftCollection.getNftItemAddressByIndex(tonlib, BigInteger.ONE);

        log.info("address at index 1 = {}", nftItem1.toString(true, true, true));
        log.info("address at index 2 = {}", nftItem2.toString(true, true, true));

        Royalty royalty = nftCollection.getRoyaltyParams(tonlib);
        Address royaltyAddress = royalty.getRoyaltyAddress();
        log.info("nft collection royalty address {}", royaltyAddress.toString(true, true, true));
    }

    public void deployNftItem(Tonlib tonlib, WalletContract wallet, BigInteger msgValue, Address nftCollectionAddress, String nftItemContentUri, TweetNaclFast.Signature.KeyPair keyPair) {

        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                nftCollectionAddress,
                msgValue,
                seqno,
                NftCollection.createMintBody(
                        0,
                        BigInteger.ZERO,
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

    private void transferNftItem(Tonlib tonlib, WalletContract wallet, BigInteger msgValue, NftItem nftItem, Address saleAddress, BigInteger forwardAmount, byte[] forwardPayload, Address responseAddress, TweetNaclFast.Signature.KeyPair keyPair) {

        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                nftItem.getAddress(),
                msgValue,
                seqno,
                NftItem.createTransferBody(
                        0,
                        saleAddress, //new owner
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
