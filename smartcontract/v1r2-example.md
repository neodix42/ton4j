# SmartContract module

### Example of usage of Wallet V1R2

```java
Tonlib tonlib = Tonlib.builder().testnet(true).build();

TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

Options options = Options.builder().publicKey(keyPair.getPublicKey()).wc(0L).build();
Wallet wallet = new Wallet(WalletVersion.V1R2, options);
WalletV1ContractR2 contract = wallet.create();

// top up non-bounceable address
String nonBounceableAddress = contract.getAddress().toString(true, true, false);

// deploy contract
contract.deploy(tonlib, keyPair.getSecretKey());

// send toncoins
contract.sendTonCoins(tonlib, keyPair.getSecretKey(), Address.of(TestFaucet.BOUNCEABLE), Utils.toNano(0.8));

// retrieve seqno
contract.getSeqno(tonlib);
```

More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/java/smartcontract) can be
found [here](../smartcontract/src/test/java/org/ton/java/smartcontract).