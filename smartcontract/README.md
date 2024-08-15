# SmartContract module

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>smartcontract</artifactId>
    <version>0.5.2</version>
</dependency>
```

## Jitpack

```xml

<dependency>
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>smartcontract</artifactId>
    <version>0.5.2</version>
</dependency>
```

## Wallets

Get familiar with various wallet types and their differences [here](README-WALLETS.md).

Currently, following wallet versions and revisions are supported:

* V1R1 [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestWalletV1R1.java)
* V1R2 [(see usage example)](v1r2-example.md)
* V1R3 [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestWalletV1R3.java)
* v2R1 [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestWalletV2R1Short.java)
* v2R2 [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestWalletV2R2Short.java)
* v3R1 [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestWalletV3R1.java)
* v3R2 [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestWalletV3R2Short.java)
* v4R2 - subscription, plugins [(see usage example)](plugin-example.md)
* Lockup - restricted [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestLockupWallet.java)
* Highload [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestHighloadWalletV2.java)
* Highload V3 [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestHighloadWalletV3.java)
* Dns [(see usage example)](dns-example.md)
* Jetton [(see usage example)](jetton-example.md)
* NFT [(see usage example)](nft-example.md)
* Payment channels [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestPayments.java)
* MultiSig [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestWalletMultiSig.java)
* Example contract [(see usage example)](sample-smc-example.md)

More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/java/smartcontract) can be
found in [here](../smartcontract/src/test/java/org/ton/java/smartcontract).

[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/smartcontract

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/smartcontract

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org