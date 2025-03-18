# Java SDK for The Open Network (TON)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Based on TON][ton-svg]][ton]
![GitHub last commit](https://img.shields.io/github/last-commit/neodiX42/ton4j)

Java libraries for interacting with TON blockchain.
Do not forget to place tonlibjson library to your project. Latest Tonlib libraries can be
found [here](https://github.com/ton-blockchain/ton/actions).

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>smartcontract</artifactId>
    <version>0.9.4</version>
</dependency>
```

## Jitpack [![JitPack][jitpack-svg]][jitpack]

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>ton4j</artifactId>
    <version>0.9.4</version>
</dependency>
```

You can use each submodule individually. Click the module below to get more details.

* [Tonlib](tonlib/README.md) - use external Tonlib shared library to communicate with TON blockchain.
* [SmartContract](smartcontract/README.md) - create and deploy custom and predefined smart-contracts.
* [Cell](cell/README.md) - create, read and manipulate Bag of Cells.
* [BitString](bitstring/README.md) - construct bit-strings.
* [Address](address/README.md) - create and parse TON wallet addresses.
* [Mnemonic](mnemonic/README.md) - helpful methods for generating deterministic keys for TON blockchain.
* [Emulator](emulator/README.md) - wrapper for using with external precompiled emulator shared library.
* [Liteclient](liteclient/README.md) - wrapper for using with external precompiled lite-client binary.
* [Fift](fift/README.md) - wrapper for using external precompiled fift binary.
* [Func](func/README.md) - wrapper for using external precompiled func binary.
* [Tolk](tolk/README.md) - wrapper for using external precompiled tolk binary.
* [TonConnect](tonconnect/README.md) - implementation of Ton Connect standard.
* [Disassembler](disassembler/README.md) - implementation of Ton Connect standard.
* [TL-B](tlb/README.md) - TL-B structures and their de/serialization.
* [Utils](utils/README.md) - create private and public keys, convert data, etc.

### Features

* ✅ BitString manipulations
* ✅ Cells serialization / deserialization
* ✅ TL-B serialization / deserialization
* ✅ Cell builder and cell slicer (reader)
* ✅ Tonlib wrapper
* ✅ Lite-client wrapper
* ✅ Fift wrapper
* ✅ Func wrapper
* ✅ Tolk wrapper
* ✅ TVM and Tx emulator wrapper
* ✅ Transaction emulator wrapper
* ✅ TonConnect
* ✅ BoC disassembler
* ✅ Extra-currency support and examples
* ✅ Support num, cell and slice as arguments for runMethod
* ✅ Render List, Tuple, Slice, Cell and Number results from runMethod
* ✅ Generate or import private key, sign, encrypt and decrypt using Tonlib
* ✅ Encrypt/decrypt with mnemonic
* ✅ Send external message
* ✅ Get block transactions
* ✅ Deploy contracts and send external messages using Tonlib
* ✅ Wallets - Simple (V1), V2, V3, V4 (plugins), V5, Lockup, ~~Highload~~/Highload-V3, DNS, Jetton, StableCoin, NFT,
  Payment-channels, ~~Multisig V1~~, Multisig V2
* ✅ HashMap, HashMapE, PfxHashMap, PfxHashMapE, HashMapAug, HashMapAugE serialization / deserialization

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=neodiX42/ton4j&type=Date)](https://star-history.com/#neodiX42/ton4j&Date)

<!-- Badges -->

[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/smartcontract

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/smartcontract

[jitpack-svg]: https://jitpack.io/v/neodiX42/ton4j.svg

[jitpack]: https://jitpack.io/#neodiX42/ton4j

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org