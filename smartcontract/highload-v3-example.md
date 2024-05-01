# SmartContract module

### Example of usage of Highload Wallet V3

```java
Tonlib tonlib=Tonlib.builder().testnet(true).build();

        TweetNaclFast.Signature.KeyPair keyPair=Utils.generateSignatureKeyPair();

        Options options=Options.builder().publicKey(keyPair.getPublicKey()).wc(0L).build();
        Wallet wallet=new Wallet(WalletVersion.highloadV3,options);
        HighloadWalletV3 contract=wallet.create();

// top up non-bounceable address
        String nonBounceableAddress=contract.getAddress().toString(true,true,false);

// deploy contract
        contract.deploy(tonlib,keyPair.getSecretKey());

// send toncoins
        contract.sendTonCoins(tonlib,keyPair.getSecretKey(),Address.of(TestFaucet.BOUNCEABLE),Utils.toNano(0.8),"comment");

// retrieve seqno
        contract.getSeqno(tonlib);
```

![Class Diagram](http://www.plantuml.com/plantuml/proxy?src=https://github.com/neodix42/ton4j/blob/highload-v3-tests/smartcontract/highload-v3.puml)

More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/java/smartcontract) can be
found [here](../smartcontract/src/test/java/org/ton/java/smartcontract).