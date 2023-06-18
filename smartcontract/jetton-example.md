# SmartContract module

## Example of usage of JettonMinter and JettonWallet classes

### Deploy Jetton Minter
```java
Options options = Options.builder()
    .adminAddress(adminWallet.getWallet().getAddress())
    .jettonContentUri("https://raw.githubusercontent.com/neodiX42/ton4j/dns-smc/1-media/neo-jetton.json")
    .jettonWalletCodeHex(JettonWallet.JETTON_WALLET_CODE_HEX)
    .build();

Wallet jettonMinter = new Wallet(WalletVersion.jettonMinter, options);
JettonMinter minter = jettonMinter.create();
log.info("jetton minter address {}", minter.getAddress().toString(true, true, true));
minter.deploy(tonlib, adminWallet.getWallet(), Utils.toNano(0.05), adminWallet.getKeyPair());

// show minter information
JettonMinterData data = minter.getJettonData(tonlib);
log.info("JettonMinterData {}", data);
log.info("minter adminAddress {}", data.getAdminAddress().toString(true, true, true));
// total supply equals to zero now
log.info("minter totalSupply {}", Utils.formatNanoValue(data.getTotalSupply()));
```

### Mint jettons
```java
// sequential calls to mint() sum up to totalSupply;
minter.mint(tonlib, adminWallet.getWallet(), adminWallet.getWallet().getAddress(), 
            Utils.toNano(0.05), Utils.toNano(0.04), Utils.toNano(100500), adminWallet.getKeyPair());

// now total supply is available
log.info("jetton total supply {}", minter.getTotalSupply(tonlib));

public void mint(
    Tonlib tonlib, 
    WalletContract adminWallet, 
    Address destination, 
    BigInteger walletMsgValue, 
    BigInteger mintMsgValue, 
    BigInteger jettonToMintAmount, 
    TweetNaclFast.Signature.KeyPair keyPair) {

    long seqno = adminWallet.getSeqno(tonlib);

    ExternalMessage extMsg = adminWallet.createTransferMessage(
        keyPair.getSecretKey(),
        this.getAddress(),
        walletMsgValue,
        seqno,
        this.createMintBody(0, destination, mintMsgValue, jettonToMintAmount)
    );
    
    tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBocNew()));
}

// owner of adminWallet holds his jettons on jettonWallet
Address adminJettonWalletAddress = minter.getJettonWalletAddress(tonlib, adminWallet.getWallet().getAddress());
log.info("admin JettonWalletAddress {}", adminJettonWalletAddress.toString(true, true, true));
```

### Edit minter content
```java
private void editMinterContent(WalletContract adminWallet, JettonMinter minter, 
                               String newUriContent, TweetNaclFast.Signature.KeyPair keyPair) {

    long seqno = adminWallet.getSeqno(tonlib);

    ExternalMessage extMsg = adminWallet.createTransferMessage(
            keyPair.getSecretKey(),
            minter.getAddress(),
            Utils.toNano(0.05),
            seqno,
            minter.createEditContentBody(newUriContent, 0));

    tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBocNew()));
}
```

### Change minter owner
```java
private void changeMinterAdmin(WalletContract adminWallet, JettonMinter minter, 
                               Address newAdmin, TweetNaclFast.Signature.KeyPair keyPair) {

    long seqno = adminWallet.getSeqno(tonlib);

    ExternalMessage extMsg = adminWallet.createTransferMessage(
            keyPair.getSecretKey(),
            minter.getAddress(),
            Utils.toNano(0.05),
            seqno,
            minter.createChangeAdminBody(0, newAdmin));

    tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBocNew()));
}
```

### Transfer jettons
```java
private void transfer(WalletContract admin, Address jettonWalletAddress, Address toAddress, 
                      BigInteger jettonAmount, TweetNaclFast.Signature.KeyPair keyPair) {
    
    long seqno = admin.getSeqno(tonlib);

    ExternalMessage extMsg = admin.createTransferMessage(
            keyPair.getSecretKey(),
            Address.of(jettonWalletAddress),
            Utils.toNano(0.05),
            seqno,
            JettonWallet.createTransferBody(
                    0, // queryId
                    jettonAmount,
                    Address.of(toAddress), // destination
                    admin.getAddress(), // response address
                    Utils.toNano("0.01"), // forward amount
                    "gift".getBytes() // forward payload
            ));

    tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBocNew()));
}
```


### Burn jettons
```java
private void burn(WalletContract admin, Address jettonWalletAddress, BigInteger jettonAmount, 
                  Address responseAddress, TweetNaclFast.Signature.KeyPair keyPair) {

    long seqno = admin.getSeqno(tonlib);

    ExternalMessage extMsg = admin.createTransferMessage(
            keyPair.getSecretKey(),
            Address.of(jettonWalletAddress),
            Utils.toNano(0.05),
            seqno,
            JettonWallet.createBurnBody(
                    0,
                    jettonAmount,
                    responseAddress
            ));

    tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBocNew()));
}
```

More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/java/smartcontract) can be
found [here](../smartcontract/src/test/java/org/ton/java/smartcontract).