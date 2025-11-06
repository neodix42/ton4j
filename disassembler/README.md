# TON VM Disassembler based on Java

> Provides Fift-like code from smart contract source.
> Latest Tonlib libraries can be
> found [here](https://github.com/ton-blockchain/ton/actions).

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>org.ton.ton4j</groupId>
    <artifactId>disassembler</artifactId>
    <version>1.3.2</version>
</dependency>
```

## Usage

```java
Tonlib tonlib = Tonlib.builder()
    .testnet(false)
    .ignoreCache(false)
    .build();

Address address = Address.of(addr);
FullAccountState accountState = tonlib.getAccountState(address);

byte[] accountStateCode = Utils.base64ToBytes(accountState.getAccount_state().getCode());

String disassembledInstruction = Disassembler.fromBoc(accountStateCode);
```

[maven-central-svg]: https://img.shields.io/maven-central/v/org.ton.ton4j/disassembler?color=red

[maven-central]: https://mvnrepository.com/artifact/org.ton.ton4j/disassembler