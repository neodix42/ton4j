# SmartContract module

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>smartcontract</artifactId>
    <version>0.0.3</version>
</dependency>
```

## Jitpack

```xml

<dependency>
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>smartcontract</artifactId>
    <version>0.0.3</version>
</dependency>
```

## Wallets

Get familiar with various wallet types and their differences [here](README-WALLETS.md).

Currently, following wallet versions and revisions are supported:

* simpleR1
* simpleR2 [(see usage example)](simple-r2-example.md)
* simpleR3 [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestWalletV1R3DeployTransfer.java)
* v2R1 [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestWalletV2R1DeployTransferShort.java)
* v2R2 [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestWalletV2R2DeployTransferShort.java)
* v3R1
* v3R2 [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestWalletV3R2DeployTransferShort.java)
* v4R1
* v4R2 - subscription, plugins [(see usage example)](plugin-example.md)
* ~~Lockup - restricted [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestLockupWalletDeployTransfer.java)~~
* Dns [(see usage example)](dns-example.md)
* Jetton [(see usage example)](jetton-example.md)
* NFT [(see usage example)](nft-example.md)
* Payment channels [(see usage example)](./src/test/java/org/ton/java/smartcontract/integrationtests/TestPayments.java) 
* Custom contract [(see usage example)](custom-smc-example.md)


More examples on how to work with [smart-contracts](../smartcontract/src/main/java/org/ton/java/smartcontract) can be
found in [here](../smartcontract/src/test/java/org/ton/java/smartcontract).

[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/smartcontract

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/smartcontract

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org