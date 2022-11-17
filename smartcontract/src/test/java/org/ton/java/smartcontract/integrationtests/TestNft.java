package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.GenerateWallet;
import org.ton.java.smartcontract.TestWallet;
import org.ton.java.smartcontract.token.nft.NftCollection;
import org.ton.java.smartcontract.token.nft.NftItem;
import org.ton.java.smartcontract.token.nft.NftMarketplace;
import org.ton.java.smartcontract.token.nft.NftSale;
import org.ton.java.smartcontract.types.*;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestNft {

    private static final String WALLET2_ADDRESS = "EQB6-6po0yspb68p7RRetC-hONAz-JwxG9514IEOKw_llXd5";

    private Address nftItem1;
    private Address nftItem2;

    static TestWallet adminWallet;

    static TestWallet nftItemBuyer;
    static Tonlib tonlib = Tonlib.builder()
            .testnet(true)
            .build();

    @BeforeClass
    public static void setUpClass() throws InterruptedException {
        adminWallet = GenerateWallet.random(tonlib, 7);
        nftItemBuyer = GenerateWallet.random(tonlib, 3);
    }

    @Test
    public void testNft() {

        log.info("admin wallet address {}", adminWallet.getWallet().getAddress().toString(true, true, true));
        log.info("buyer wallet address {}", nftItemBuyer.getWallet().getAddress().toString(true, true, true));

        Options optionsNftCollection = Options.builder()
                .adminAddress(adminWallet.getWallet().getAddress())
                .royalty(0.13)
                .royaltyAddress(adminWallet.getWallet().getAddress())
                .collectionContentUri("https://raw.githubusercontent.com/neodiX42/ton4j/dns-smc/1-media/nft-collection.json")
                .collectionContentBaseUri("https://raw.githubusercontent.com/neodiX42/ton4j/dns-smc/1-media/")
                .nftItemCodeHex(NftItem.NFT_ITEM_CODE_HEX)
                .build();

        Wallet nftCollectionWallet = new Wallet(WalletVersion.nftCollection, optionsNftCollection);
        NftCollection nftCollection = nftCollectionWallet.create();
        log.info("NFT collection address {}", nftCollection.getAddress().toString(true, true, true));

        nftCollection.deploy(tonlib, adminWallet.getWallet(), Utils.toNano(1), adminWallet.getKeyPair());

        Utils.sleep(15, "deploying NFT collection");
        getNftCollectionInfo(nftCollection);

        // create and deploy NFT Item
        deployNftItem(tonlib, adminWallet.getWallet(), BigInteger.ZERO, Utils.toNano(0.06), nftCollection.getAddress(), "nft-item-1.json", adminWallet.getKeyPair());
        Utils.sleep(25, "deploying NFT item #1");

        deployNftItem(tonlib, adminWallet.getWallet(), BigInteger.ONE, Utils.toNano(0.07), nftCollection.getAddress(), "nft-item-2.json", adminWallet.getKeyPair());
        Utils.sleep(25, "deploying NFT item #2");

        assertThat(getNftCollectionInfo(nftCollection)).isEqualTo(2);

        NftItem item1 = new NftItem(Options.builder().address(nftItem1).build());
        NftItem item2 = new NftItem(Options.builder().address(nftItem2).build());

        // deploy own nft marketplace
        NftMarketplace marketplace = new NftMarketplace(Options.builder().adminAddress(adminWallet.getWallet().getAddress()).build());
        log.info("nft marketplace address {}", marketplace.getAddress().toString(true, true, true));
        marketplace.deploy(tonlib, adminWallet.getWallet(), Utils.toNano(1.1), adminWallet.getKeyPair());
        Utils.sleep(20, "deploying nft marketplace");

        //deploy nft sale for item 1
        Options optionsNftSale1 = Options.builder()
                .marketplaceAddress(marketplace.getAddress())
                .nftItemAddress(Address.of(nftItem1))
                .fullPrice(Utils.toNano(1.1))
                .marketplaceFee(Utils.toNano(0.4))
                .royaltyAddress(nftCollection.getAddress())
                .royaltyAmount(Utils.toNano(0.3))
                .build();

        NftSale nftSale1 = new NftSale(optionsNftSale1);
        log.info("nft-sale-1 address {}", nftSale1.getAddress().toString(true, true, true));
        nftSale1.deploy(tonlib, adminWallet.getWallet(), Utils.toNano(0.06), marketplace.getAddress(), adminWallet.getKeyPair());
        Utils.sleep(25, "deploying NFT sale smart-contract for nft item #1");

        // get nft item 1 data
        log.info("nftSale data for nft item #1 {}", nftSale1.getData(tonlib));

        //deploy nft sale for item 2
        Options optionsNftSale2 = Options.builder()
                .marketplaceAddress(marketplace.getAddress())
                .nftItemAddress(Address.of(nftItem2))
                .fullPrice(Utils.toNano(1.2))
                .marketplaceFee(Utils.toNano(0.3))
                .royaltyAddress(nftCollection.getAddress())
                .royaltyAmount(Utils.toNano(0.2))
                .build();

        NftSale nftSale2 = new NftSale(optionsNftSale2);
        log.info("nft-sale-2 address {}", nftSale2.getAddress().toString(true, true, true));
        nftSale2.deploy(tonlib, adminWallet.getWallet(), Utils.toNano(0.06), marketplace.getAddress(), adminWallet.getKeyPair());
        Utils.sleep(25, "deploying NFT sale smart-contract for nft item #2");

        // get nft item 2 data
        log.info("nftSale data for nft item #2 {}", nftSale2.getData(tonlib));

        //sends from adminWallet to nftItem request for static data, response comes to adminWallet
        //https://github.com/ton-blockchain/token-contract/blob/main/nft/nft-item.fc#L131
        getStaticData(tonlib, adminWallet.getWallet(), Utils.toNano(0.088), item1.getAddress(), 661, adminWallet.getKeyPair());

        // transfer nft item to nft sale smart-contract (send amount > full_price+1ton)
        transferNftItem(tonlib, adminWallet.getWallet(), Utils.toNano(1.4), item1, nftSale1.getAddress(), Utils.toNano(0.02), "gift1".getBytes(), adminWallet.getWallet().getAddress(), adminWallet.getKeyPair());
        Utils.sleep(30, "transferring item-1 to nft-sale-1 and waiting for seqno update");

        transferNftItem(tonlib, adminWallet.getWallet(), Utils.toNano(1.5), item2, nftSale2.getAddress(), Utils.toNano(0.02), "gift2".getBytes(), adminWallet.getWallet().getAddress(), adminWallet.getKeyPair());
        Utils.sleep(30, "transferring item-2 to nft-sale-2 and waiting for seqno update");

        // cancels selling of item1, moves nft-item from nft-sale-1 smc back to adminWallet. nft-sale-1 smc becomes uninitialized
        nftSale1.cancel(tonlib, adminWallet.getWallet(), Utils.toNano(1), nftSale1.getAddress(), 0, adminWallet.getKeyPair());
        Utils.sleep(25);

        // buy nft-item-2. send fullPrice+minimalGasAmount(1ton)
        nftItemBuyer.getWallet().sendTonCoins(tonlib, nftItemBuyer.getKeyPair().getSecretKey(), nftSale2.getAddress(), Utils.toNano(1.2 + 1));

        //after changed owner this will fail with 401 error - current nft collection is not editable, so nothing happens
        editNftCollectionContent(tonlib, adminWallet.getWallet(), Utils.toNano(0.055), nftCollection.getAddress(), "ton://my-nft/collection.json", "ton://my-nft/", 0.16, Address.of(WALLET2_ADDRESS), adminWallet.getKeyPair());

        changeNftCollectionOwner(tonlib, adminWallet.getWallet(), Utils.toNano(0.06), nftCollection.getAddress(), Address.of(WALLET2_ADDRESS), adminWallet.getKeyPair());

        getRoyaltyParams(tonlib, adminWallet.getWallet(), Utils.toNano(0.0777), nftCollection.getAddress(), adminWallet.getKeyPair());
    }

    private long getNftCollectionInfo(NftCollection nftCollection) {
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

        return data.getNextItemIndex();
    }

    private void deployNftItem(Tonlib tonlib, WalletContract wallet, BigInteger index, BigInteger msgValue, Address nftCollectionAddress, String nftItemContentUri, TweetNaclFast.Signature.KeyPair keyPair) {

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

        tonlib.sendRawMessage(extMsg.message.toBocBase64(false));
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

        tonlib.sendRawMessage(extMsg.message.toBocBase64(false));
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

        tonlib.sendRawMessage(extMsg.message.toBocBase64(false));
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

        tonlib.sendRawMessage(extMsg.message.toBocBase64(false));
    }

    private void getNftItemInfo(NftCollection nftCollection, NftItem nftItem) {
        ItemData data = nftCollection.getNftItemContent(tonlib, nftItem);
        log.info("nftItem {}", data);
    }

    private void getSingleNftItemInfo(NftItem nftItem) {
        ItemData data = nftItem.getData(tonlib);
        log.info("nftItem {}", data);
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
                        nftSaleAddress,
                        forwardAmount,
                        forwardPayload,
                        responseAddress)
        );

        tonlib.sendRawMessage(extMsg.message.toBocBase64(false));
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

        tonlib.sendRawMessage(extMsg.message.toBocBase64(false));
    }
}
