package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.GenerateWallet;
import org.ton.java.smartcontract.token.nft.NftCollection;
import org.ton.java.smartcontract.token.nft.NftItem;
import org.ton.java.smartcontract.token.nft.NftMarketplace;
import org.ton.java.smartcontract.token.nft.NftSale;
import org.ton.java.smartcontract.types.CollectionData;
import org.ton.java.smartcontract.types.Royalty;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.wallet.v3.WalletV3R1;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestNft extends CommonTest {

    private static final String WALLET2_ADDRESS = "EQB6-6po0yspb68p7RRetC-hONAz-JwxG9514IEOKw_llXd5";

    static WalletV3R1 adminWallet;

    static WalletV3R1 nftItemBuyer;

    private Address nftItem1Address;
    private Address nftItem2Address;

    @BeforeClass
    public static void setUpClass() throws InterruptedException {
        adminWallet = GenerateWallet.random(tonlib, 7);
        nftItemBuyer = GenerateWallet.random(tonlib, 3);
    }

    @Test
    public void testNft() {

        log.info("admin wallet address {}", adminWallet.getAddress());
        log.info("buyer wallet address {}", nftItemBuyer.getAddress());

        NftCollection nftCollection = NftCollection.builder()
                .tonlib(tonlib)
                .adminAddress(adminWallet.getAddress())
                .royalty(0.13)
                .royaltyAddress(adminWallet.getAddress())
                .collectionContentUri("https://raw.githubusercontent.com/neodiX42/ton4j/main/1-media/nft-collection.json")
                .collectionContentBaseUri("https://raw.githubusercontent.com/neodiX42/ton4j/main/1-media/")
                .nftItemCodeHex(WalletCodes.nftItem.getValue())
                .build();

        log.info("NFT collection address {}", nftCollection.getAddress());

        // deploy NFT Collection
        WalletV3Config adminWalletConfig = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(nftCollection.getAddress())
                .amount(Utils.toNano(1))
                .stateInit(nftCollection.getStateInit())
                .build();

        ExtMessageInfo extMessageInfo = adminWallet.sendTonCoins(adminWalletConfig);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        log.info("deploying NFT collection");

        nftCollection.waitForDeployment(60);

        getNftCollectionInfo(nftCollection);

        Cell body = NftCollection.createMintBody(
                0,
                0,
                Utils.toNano(0.06),
                adminWallet.getAddress(),
                "nft-item-1.json");

        adminWalletConfig = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(nftCollection.getAddress())
                .amount(Utils.toNano(1))
                .body(body)
                .build();

        extMessageInfo = adminWallet.sendTonCoins(adminWalletConfig);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(30, "deploying NFT item #1");

        body = NftCollection.createMintBody(
                0,
                1,
                Utils.toNano(0.07),
                adminWallet.getAddress(),
                "nft-item-2.json");

        adminWalletConfig = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(nftCollection.getAddress())
                .amount(Utils.toNano(1))
                .body(body)
                .build();

        extMessageInfo = adminWallet.sendTonCoins(adminWalletConfig);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(40, "deploying NFT item #2");

        assertThat(getNftCollectionInfo(nftCollection)).isEqualTo(2);

        NftMarketplace marketplace = NftMarketplace.builder()
                .adminAddress(adminWallet.getAddress())
                .build();

        log.info("nft marketplace address {}", marketplace.getAddress());

        // deploy own NFT marketplace
        adminWalletConfig = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(marketplace.getAddress())
                .amount(Utils.toNano(1))
                .stateInit(marketplace.getStateInit())
                .build();

        extMessageInfo = adminWallet.sendTonCoins(adminWalletConfig);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(30, "deploying nft marketplace");

        // deploy nft sale for item 1
        NftSale nftSale1 = NftSale.builder()
                .marketplaceAddress(marketplace.getAddress())
                .nftItemAddress(nftItem1Address)
                .fullPrice(Utils.toNano(1.1))
                .marketplaceFee(Utils.toNano(0.4))
                .royaltyAddress(nftCollection.getAddress())
                .royaltyAmount(Utils.toNano(0.3))
                .build();

        log.info("nft-sale-1 address {}", nftSale1.getAddress());

        body = CellBuilder.beginCell()
                .storeUint(1, 32)
                .storeCoins(Utils.toNano(0.06))
                .storeRef(nftSale1.getStateInit().toCell())
                .storeRef(CellBuilder.beginCell().endCell())
                .endCell();

        adminWalletConfig = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(marketplace.getAddress())
                .amount(Utils.toNano(0.06))
                .body(body)
                .build();

        extMessageInfo = adminWallet.sendTonCoins(adminWalletConfig);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(40, "deploying NFT sale smart-contract for nft item #1");

        // get nft item 1 data
        log.info("nftSale data for nft item #1 {}", nftSale1.getData(tonlib));

        //deploy nft sale for item 2 -----------------------------------------------------------
        NftSale nftSale2 = NftSale.builder()
                .marketplaceAddress(marketplace.getAddress())
                .nftItemAddress(Address.of(nftItem2Address))
                .fullPrice(Utils.toNano(1.2))
                .marketplaceFee(Utils.toNano(0.3))
                .royaltyAddress(nftCollection.getAddress())
                .royaltyAmount(Utils.toNano(0.2))
                .build();

        body = CellBuilder.beginCell()
                .storeUint(1, 32)
                .storeCoins(Utils.toNano(0.06))
                .storeRef(nftSale2.getStateInit().toCell())
                .storeRef(CellBuilder.beginCell().endCell())
                .endCell();

        adminWalletConfig = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(marketplace.getAddress())
                .amount(Utils.toNano(0.06))
                .body(body)
                .build();

        log.info("nft-sale-2 address {}", nftSale2.getAddress().toString(true, true, true));
        extMessageInfo = adminWallet.sendTonCoins(adminWalletConfig);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(40, "deploying NFT sale smart-contract for nft item #2");

        // get nft item 2 data
        log.info("nftSale data for nft item #2 {}", nftSale2.getData(tonlib));

        //sends from adminWallet to nftItem request for static data, response comes to adminWallet
        //https://github.com/ton-blockchain/token-contract/blob/main/nft/nft-item.fc#L131
        getStaticData(adminWallet, Utils.toNano(0.088), nftItem1Address, BigInteger.valueOf(661), adminWallet.getKeyPair());

        // transfer nft item to nft sale smart-contract (send amount > full_price+1ton)
        transferNftItem(adminWallet, Utils.toNano(1.4), nftItem1Address, BigInteger.ZERO, nftSale1.getAddress(),
                Utils.toNano(0.02), "gift1".getBytes(), adminWallet.getAddress());
        Utils.sleep(35, "transferring item-1 to nft-sale-1 and waiting for seqno update");

        transferNftItem(adminWallet, Utils.toNano(1.5), nftItem2Address, BigInteger.ZERO, nftSale2.getAddress(),
                Utils.toNano(0.02), "gift2".getBytes(), adminWallet.getAddress());
        Utils.sleep(35, "transferring item-2 to nft-sale-2 and waiting for seqno update");

        // cancels selling of item1, moves nft-item from nft-sale-1 smc back to adminWallet. nft-sale-1 smc becomes uninitialized

        WalletV3Config walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(adminWallet.getSeqno())
                .destination(nftSale1.getAddress())
                .amount(Utils.toNano(1))
                .body(NftSale.createCancelBody(0))
                .build();
        extMessageInfo = adminWallet.sendTonCoins(walletV3Config);

        assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(35, "cancel selling of item1");

        // buy nft-item-2. send fullPrice+minimalGasAmount(1ton)
        walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(nftItemBuyer.getSeqno())
                .destination(nftSale2.getAddress())
                .amount(Utils.toNano(1.2 + 1))
                .build();
        extMessageInfo = nftItemBuyer.sendTonCoins(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        // after changed owner this will fail with 401 error - current nft collection is not editable, so nothing happens
        editNftCollectionContent(adminWallet, Utils.toNano(0.055), nftCollection.getAddress(),
                "ton://my-nft/collection.json", "ton://my-nft/", 0.16,
                Address.of(WALLET2_ADDRESS), adminWallet.getKeyPair());

        changeNftCollectionOwner(adminWallet, Utils.toNano(0.06), nftCollection.getAddress(),
                Address.of(WALLET2_ADDRESS), adminWallet.getKeyPair());

        getRoyaltyParams(adminWallet, Utils.toNano(0.0777), nftCollection.getAddress());
    }

    private long getNftCollectionInfo(NftCollection nftCollection) {
        CollectionData data = nftCollection.getCollectionData(tonlib);
        log.info("nft collection info {}", data);
        log.info("nft collection item count {}", data.getNextItemIndex());
        log.info("nft collection owner {}", data.getOwnerAddress());

        nftItem1Address = nftCollection.getNftItemAddressByIndex(tonlib, BigInteger.ZERO);
        nftItem2Address = nftCollection.getNftItemAddressByIndex(tonlib, BigInteger.ONE);

        log.info("address at index 1 = {}", nftItem1Address);
        log.info("address at index 2 = {}", nftItem2Address);

        Royalty royalty = nftCollection.getRoyaltyParams(tonlib);
        log.info("nft collection royalty address {}", royalty.getRoyaltyAddress());

        return data.getNextItemIndex();
    }


    public void changeNftCollectionOwner(WalletV3R1 wallet, BigInteger msgValue, Address nftCollectionAddress, Address newOwner, TweetNaclFast.Signature.KeyPair keyPair) {

        WalletV3Config walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(wallet.getSeqno())
                .destination(nftCollectionAddress)
                .amount(msgValue)
                .body(NftCollection.createChangeOwnerBody(0, newOwner))
                .build();
        ExtMessageInfo extMessageInfo = wallet.sendTonCoins(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
    }

    public void editNftCollectionContent(WalletV3R1 wallet, BigInteger msgValue, Address nftCollectionAddress,
                                         String collectionContentUri, String nftItemContentBaseUri, double royalty,
                                         Address royaltyAddress, TweetNaclFast.Signature.KeyPair keyPair) {

        WalletV3Config walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(wallet.getSeqno())
                .destination(nftCollectionAddress)
                .amount(msgValue)
                .body(NftCollection.createEditContentBody(
                        0,
                        collectionContentUri,
                        nftItemContentBaseUri,
                        royalty,
                        royaltyAddress))
                .build();
        ExtMessageInfo extMessageInfo = wallet.sendTonCoins(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
    }

    public void getRoyaltyParams(WalletV3R1 wallet, BigInteger msgValue, Address nftCollectionAddress) {

        WalletV3Config walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(wallet.getSeqno())
                .destination(nftCollectionAddress)
                .amount(msgValue)
                .body(NftCollection.createGetRoyaltyParamsBody(0))
                .build();
        ExtMessageInfo extMessageInfo = wallet.sendTonCoins(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
    }

    private void transferNftItem(WalletV3R1 wallet, BigInteger msgValue, Address nftItemAddress, BigInteger queryId, Address nftSaleAddress,
                                 BigInteger forwardAmount, byte[] forwardPayload, Address responseAddress) {

        WalletV3Config walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(wallet.getSeqno())
                .destination(nftItemAddress)
                .amount(msgValue)
                .body(NftItem.createTransferBody(
                        queryId,
                        nftSaleAddress,
                        forwardAmount,
                        forwardPayload,
                        responseAddress))
                .build();
        ExtMessageInfo extMessageInfo = wallet.sendTonCoins(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
    }

    private void getStaticData(WalletV3R1 wallet, BigInteger msgValue, Address nftItemAddress, BigInteger queryId, TweetNaclFast.Signature.KeyPair keyPair) {
        WalletV3Config config = WalletV3Config.builder()
                .walletId(42)
                .seqno(wallet.getSeqno())
                .destination(nftItemAddress)
                .amount(msgValue)
                .body(NftItem.createGetStaticDataBody(queryId))
                .build();
        ExtMessageInfo extMessageInfo = wallet.sendTonCoins(config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
    }
}
