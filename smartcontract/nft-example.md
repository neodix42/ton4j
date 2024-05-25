# SmartContract module

## Example of usage of NftItem, NftSale, NftCollection and NftMarketplace classes

### Deploy NFT collection with offchain metadata

Make sure nft collection metadata file is available online.

```java
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

ExtMessageInfo extMessageInfo = adminWallet.send(adminWalletConfig);
assertThat(extMessageInfo.getError().getCode()).isZero();
log.info("deploying NFT collection");
```

### Retrieve NFT collection information

```java
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
```

### Deploy NFT item with offchain metadata

Make sure `"nft-item-1.json"` is located under path `collectionContentBaseUri`

```java
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

extMessageInfo = adminWallet.send(adminWalletConfig);
assertThat(extMessageInfo.getError().getCode()).isZero();
log.info("deploying NFT item #1");
}
```

### Sell NFT item

To sell NFT item you need first to deploy nft-sale smart-contract with attached NFT item and then
transfer that NFT item to it.

#### Deploy nft-sale smart-contract

```java
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

extMessageInfo = adminWallet.send(adminWalletConfig);
assertThat(extMessageInfo.getError().getCode()).isZero();
log.inf("deploying NFT sale smart-contract for nft item #1");
```

#### Transfer NFT item to nft-sale smart-contract

```java
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
ExtMessageInfo extMessageInfo = wallet.send(walletV3Config);
assertThat(extMessageInfo.getError().getCode()).isZero();
```

More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/java/smartcontract) can be
found [here](../smartcontract/src/test/java/org/ton/java/smartcontract).