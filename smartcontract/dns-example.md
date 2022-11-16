# SmartContract module

## Example of usage of Dns class

### Resolve domain name
```java
Tonlib tonlib = Tonlib.builder().build();
Dns dns = new Dns(tonlib);
Address rootAddress = dns.getRootDnsAddress();
log.info("root DNS address = {}", rootAddress.toString(true, true, true));

Object result = dns.resolve("apple.ton", DNS_CATEGORY_NEXT_RESOLVER, true);
log.info("apple.ton resolved to {}", ((Address) result).toString(true, true, true));

Address addr = (Address) dns.getWalletAddress("foundation.ton");
log.info("foundation.ton resolved to {}", addr.toString(true, true, true));
```

### Deploy own root DNS smart-contract

```java
TestWallet adminWallet = GenerateWallet.random(tonlib, 15);

DnsRoot dnsRoot = new DnsRoot();
log.info("new root DNS address {}", dnsRoot.getAddress().toString(true, true, true));

dnsRoot.deploy(tonlib, adminWallet.getWallet(), adminWallet.getKeyPair());
```
### Deploy DNS collection
```java
Options optionsDnsCollection = Options.builder()
        .collectionContent(NftUtils.createOffchainUriCell("https://raw.githubusercontent.com/neodiX42/ton4j/dns-smc/1-media/dns-collection-2.json"))
        .dnsItemCodeHex(dnsItemCodeHex)
        .code(Cell.fromBoc(dnsCollectionCodeHex))
        .build();

Wallet dnsCollectionWallet = new Wallet(WalletVersion.dnsCollection, optionsDnsCollection);
DnsCollection dnsCollection = dnsCollectionWallet.create();
log.info("DNS collection address {}", dnsCollection.getAddress().toString(true, true, true));

dnsCollection.deploy(tonlib, adminWallet.getWallet(), Utils.toNano(0.5), adminWallet.getKeyPair());

//show dns collection information
CollectionData data = dnsCollection.getCollectionData(tonlib);
log.info("dns collection info {}", data);
```

### Deploy DNS item to the collection
```java
private void deployDnsItem(Tonlib tonlib, WalletContract adminWallet, BigInteger msgValue, 
                           Address dnsCollectionAddress, String domainName, TweetNaclFast.Signature.KeyPair keyPair) {

    long seqno = adminWallet.getSeqno(tonlib);

    CellBuilder body = CellBuilder.beginCell();
    body.storeUint(0, 32);  // OP deploy new nft
    body.storeRef(CellBuilder.beginCell().storeString(domainName).endCell());

    ExternalMessage extMsg = adminWallet.createTransferMessage(
        keyPair.getSecretKey(),
        dnsCollectionAddress,
        msgValue,
        seqno,
        body.endCell()
    );

    tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
}
```

### Show deployed DNS item information
```java
private void getDnsItemInfo(DnsCollection dnsCollection, DnsItem dnsItem) {
    ItemData data = dnsCollection.getNftItemContent(tonlib, dnsItem);
    log.info("dns item data {}", data);
    log.info("dns item collection address {}", data.getCollectionAddress().toString(true, true, true));
    if (nonNull(data.getOwnerAddress())) {
        log.info("dns item owner address {}", data.getOwnerAddress().toString(true, true, true));
    }

    if (isNull(data.getOwnerAddress())) { // auction is opened
        AuctionInfo auctionInfo = dnsItem.getAuctionInfo(tonlib);
        Address maxBidAddress = auctionInfo.getMaxBidAddress();
        BigInteger maxBidAmount = auctionInfo.getMaxBidAmount();
        log.info("AUCTION: maxBid {}, maxBidAddress {}, endTime {}", Utils.formatNanoValue(maxBidAmount), 
                    maxBidAddress.toString(true, true, true), Utils.toUTC(auctionInfo.getAuctionEndTime()));
    } else {
        log.info("SOLD to {}", data.getOwnerAddress().toString(true, true, true));
    }
    String domain = dnsItem.getDomain(tonlib);
    log.info("domain {}", domain);

    long lastFillUpTime = dnsItem.getLastFillUpTime(tonlib);
    log.info("lastFillUpTime {}, {}", lastFillUpTime, Utils.toUTC(lastFillUpTime));
}
```

### Make a bid on auction
In order to purchase a domain name you have to make a bid on auction for the interested DNS item. 
This can be done by sending toncoins to the DNS item smart-contract address. 
```java
buyerWallet.getWallet().sendTonCoins(tonlib, buyerWallet.getKeyPair().getSecretKey(), dnsItem1Address, Utils.toNano(13));
```

### Claim your domain
Once auction has ended, and you won it, you can claim the domain name by executing any of two operations below.

```java
// assign your wallet to a new dns item, so it could resolve your wallet address to your-domain.ton
private void changeDnsRecord(TestWallet ownerWallet, DnsItem dnsItem, Address newSmartContract) {
    long seqno = ownerWallet.getWallet().getSeqno(tonlib);

    ExternalMessage extMsg = ownerWallet.getWallet().createTransferMessage(
        ownerWallet.getKeyPair().getSecretKey(),
        dnsItem.getAddress(), // toAddress
        Utils.toNano(0.07),
        seqno,
        DnsItem.createChangeContentEntryBody(DNS_CATEGORY_WALLET,
        DnsUtils.createSmartContractAddressRecord(newSmartContract),
        0)
    );

    tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
}
```
```java
// or just invoke static data report
private void getStaticData(TestWallet ownerWallet, DnsItem dnsItem) {
    long seqno = ownerWallet.getWallet().getSeqno(tonlib);

    ExternalMessage extMsg = ownerWallet.getWallet().createTransferMessage(
        ownerWallet.getKeyPair().getSecretKey(),
        dnsItem.getAddress(), // toAddress
        Utils.toNano(0.05),
        seqno,
        dnsItem.createStaticDataBody(661)
    );

    tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
}
```

### Transfer DNS item
By transferring DNS item you are changing its owner address to a recipient's address.
```java
private void transferDnsItem(TestWallet ownerWallet, DnsItem dnsItem, String newOwner) {
    long seqno = ownerWallet.getWallet().getSeqno(tonlib);

    ExternalMessage extMsg = ownerWallet.getWallet().createTransferMessage(
            ownerWallet.getKeyPair().getSecretKey(),
            dnsItem.getAddress(), // toAddress
            Utils.toNano(0.08),
            seqno,
            dnsItem.createTransferBody(
                    0,
                    Address.of(newOwner),
                    Utils.toNano(0.02),
                    "gift".getBytes(),
                    ownerWallet.getWallet().getAddress()));

    tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
}
```

### Release DNS item
```java
// msg_value >= min_price and obviously only the owner can release the domain;
// once it is released the owner changed to null and auction available again;
private void releaseDnsItem(TestWallet ownerWallet, DnsItem dnsItem, BigInteger amount) {
    long seqno = ownerWallet.getWallet().getSeqno(tonlib);

    CellBuilder payload = CellBuilder.beginCell();
    payload.storeUint(0x4ed14b65, 32); // op::dns_balance_release = 0x4ed14b65;
    payload.storeUint(123, 64);

    ExternalMessage extMsg = ownerWallet.getWallet().createTransferMessage(
        ownerWallet.getKeyPair().getSecretKey(),
        dnsItem.getAddress(), // toAddress
        amount,
        seqno,
        payload.endCell()
    );

    tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
}
```
More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/java/smartcontract) can be
found [here](../smartcontract/src/test/java/org/ton/java/smartcontract).