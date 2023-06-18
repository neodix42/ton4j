# SmartContract module

## Example of usage of NftItem, NftSale, NftCollection and NftMarketplace classes

### Deploy NFT collection with offchain metadata 
Make sure nft collection metadata file is available online. 
```java
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
```

### Retrieve NFT collection information
```java
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
```


### Deploy NFT item with offchain metadata
Make sure `"nft-item-1.json"` is located under path `collectionContentBaseUri`
```java
deployNftItem(tonlib, adminWallet.getWallet(), BigInteger.ZERO, 
              Utils.toNano(0.06), nftCollection.getAddress(), 
              "nft-item-1.json", adminWallet.getKeyPair());

private void deployNftItem(Tonlib tonlib, WalletContract wallet, BigInteger index, BigInteger msgValue, 
                           Address nftCollectionAddress, String nftItemContentUri, TweetNaclFast.Signature.KeyPair keyPair) {

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
        nftItemContentUri)
    );
    
    tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBocNew()));
}
```


### Deploy own NFT marketplace
You can sell NFT items at NFT marketplace or without it.
For that you can deploy your own NFT marketplace or use existing one. 
```java
NftMarketplace marketplace = new NftMarketplace(Options.builder().adminAddress(adminWallet.getWallet().getAddress()).build());
log.info("nft marketplace address {}", marketplace.getAddress().toString(true, true, true));
marketplace.deploy(tonlib, adminWallet.getWallet(), Utils.toNano(1.1), adminWallet.getKeyPair());
```
or use existing one

### Sell NFT item
To sell NFT item you need first to deploy nft-sale smart-contract with attached NFT item and then
transfer that NFT item to it.
#### Deploy nft-sale smart-contract
```java
Options optionsNftSale1 = Options.builder()
        .marketplaceAddress(marketplace.getAddress()) // use own marketplace or for example nft makretplace from getgems.io
        .nftItemAddress(Address.of(nftItem1))
        .fullPrice(Utils.toNano(1.1))
        .marketplaceFee(Utils.toNano(0.4))
        .royaltyAddress(nftCollection.getAddress())
        .royaltyAmount(Utils.toNano(0.3))
    .build();

NftSale nftSale1 = new NftSale(optionsNftSale1);
log.info("nft-sale-1 address {}", nftSale1.getAddress().toString(true, true, true));
nftSale1.deploy(tonlib, adminWallet.getWallet(), Utils.toNano(0.06), marketplace.getAddress(), adminWallet.getKeyPair());
```
#### Transfer NFT item to nft-sale smart-contract
```java
transferNftItem(tonlib, adminWallet.getWallet(), Utils.toNano(1.4), item1, nftSale1.getAddress(), Utils.toNano(0.02), 
                "gift1".getBytes(), adminWallet.getWallet().getAddress(), adminWallet.getKeyPair());

private void transferNftItem(Tonlib tonlib, WalletContract wallet, BigInteger msgValue, NftItem nftItem, 
        Address nftSaleAddress, BigInteger forwardAmount, byte[] forwardPayload, Address responseAddress, 
        TweetNaclFast.Signature.KeyPair keyPair) {

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
            responseAddress
        )
    );

    tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBocNew()));
}
```

### Cancel sell off of NFT item
Below cancels sell off of NFT item, moves it from nft-sale smart-contract back to adminWallet. 
nft-sale smart-contract becomes uninitialized.
```java
nftSale1.cancel(tonlib, adminWallet.getWallet(), Utils.toNano(1), nftSale1.getAddress(), 0, adminWallet.getKeyPair());
```

### Buy NFT item
In order to buy NFT item one should send `fullPrice + minimalGasAmount (1 Toncoin)` amount of toncoins from his wallet to nft-sale smart-contract.
```java
nftItemBuyerWallet.sendTonCoins(tonlib, nftItemBuyerWallet.getSecretKey(), nftSale2.getAddress(), Utils.toNano(1.2 + 1));
```

### Edit NFT collection content
If you are the owner of NFT collection you can edit its content. Please notice, that there are several types of NFT collection smart-contracts. 
In current version non-editable NFT collection is used and thus cannot be edited. 
```java
editNftCollectionContent(tonlib, adminWallet.getWallet(), Utils.toNano(0.055), nftCollection.getAddress(), 
        "ton://my-nft/collection.json", "ton://my-nft/", 0.16, Address.of(WALLET2_ADDRESS), adminWallet.getKeyPair());

public void editNftCollectionContent(Tonlib tonlib, WalletContract wallet, BigInteger msgValue, Address nftCollectionAddress, 
        String collectionContentUri, String nftItemContentBaseUri, double royalty, Address royaltyAddress, 
        TweetNaclFast.Signature.KeyPair keyPair) {

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
            royaltyAddress
        )
    );
        
    tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBocNew()));
}
```

### Change NFT collection owner

```java
public void changeNftCollectionOwner(Tonlib tonlib, WalletContract wallet, BigInteger msgValue, Address nftCollectionAddress, 
                                     Address newOwner, TweetNaclFast.Signature.KeyPair keyPair) {

    long seqno = wallet.getSeqno(tonlib);

    ExternalMessage extMsg = wallet.createTransferMessage(
            keyPair.getSecretKey(),
            nftCollectionAddress,
            msgValue,
            seqno,
            NftCollection.createChangeOwnerBody(0, newOwner)
    );

    tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBocNew()));
}
```

More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/java/smartcontract) can be
found [here](../smartcontract/src/test/java/org/ton/java/smartcontract).