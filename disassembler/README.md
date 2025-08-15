# TON VM Disassembler based on Java

> Provides Fift-like code from smart contract source.
> Latest Tonlib libraries can be
> found [here](https://github.com/ton-blockchain/ton/actions).

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>disassembler</artifactId>
    <version>1.1.0</version>
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

[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/disassembler?color=red

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/disassembler