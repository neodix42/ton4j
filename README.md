# Java SDK for The Open Network (TON)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Based on TON][ton-svg]][ton]
![GitHub last commit](https://img.shields.io/github/last-commit/ton-blockchain/ton4j)
![](https://tokei.rs/b1/github/ton-blockchain/ton4j?category=code)
![](https://tokei.rs/b1/github/ton-blockchain/ton4j?category=files)


Java libraries and wrapper for interacting with TON blockchain. ton4j requires minimum `Java 11`.

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>org.ton.ton4j</groupId>
    <artifactId>smartcontract</artifactId>
    <version>1.3.2</version>
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
    <groupId>org.ton.ton4j</groupId>
    <artifactId>ton4j</artifactId>
    <version>1.3.2</version>
</dependency>
```

You can use each submodule individually. Click the module below to get more details.

* [Tonlib](tonlib/README.md) - use external Tonlib shared library to communicate with TON blockchain.
* [Adnl](adnl/README.md) - Lite-client based on native ADNL protocol.
* [SmartContract](smartcontract/README.md) - create and deploy custom and predefined smart-contracts.
* [Cell](cell/README.md) - create, read and manipulate Bag of Cells.
* [BitString](bitstring/README.md) - construct bit-strings.
* [Address](address/README.md) - create and parse TON wallet addresses.
* [Mnemonic](mnemonic/README.md) - helpful methods for generating deterministic keys for TON blockchain.
* [Emulator](emulator/README.md) - wrapper for using with external precompiled emulator shared library.
* [Exporter](exporter/README.md) - TON database reader/exporter that uses RocksDB Java JNA libraries.
* [Liteclient](liteclient/README.md) - wrapper for using with external precompiled lite-client binary.
* [TonCenter Client V2](toncenter/README.md) - wrapper used to send REST calls towards [TonCenter API v2](https://toncenter.com/api/v2/) .
* [TonCenter Client V3](toncenter-indexer-v3/README.md) - wrapper used to send REST calls towards [TonCenter Indexer API v3](https://toncenter.com/api/v3/) .
* [Fift](fift/README.md) - wrapper for using external precompiled fift binary.
* [Func](func/README.md) - wrapper for using external precompiled func binary.
* [Tolk](tolk/README.md) - wrapper for using external precompiled tolk binary.
* [TonConnect](tonconnect/README.md) - implementation of Ton Connect standard.
* [Disassembler](disassembler/README.md) - implementation of Ton Connect standard.
* [TL-B](tlb/README.md) - TL-B structures and their de/serialization.
* [TL](tl/README.md) - TL structures and their de/serialization. Used mainly for lite-server queries and responses as well as for RockDB key/values. 
* [Utils](utils/README.md) - create private and public keys, convert data, etc.

### Features

* ✅ BitString manipulations
* ✅ Cells serialization / deserialization
* ✅ TL-B serialization / deserialization
* ✅ TL serialization / deserialization
* ✅ Cell builder and cell slicer (reader)
* ✅ Tonlib, Lite-client, TVM/TX, Fift, Func and Tolk wrappers
* ✅ ADNL Lite-client
* ✅ TON RocksDB direct access
* ✅ TonConnect
* ✅ TonCenter V2 wrapper
* ✅ TonCenter Indexer V3 wrapper
* ✅ Fift, Func, Tolk wrappers
* ✅ BoC disassembler
* ✅ Extra-currency support and examples
* ✅ Support num, cell and slice as arguments for runMethod
* ✅ Render List, Tuple, Slice, Cell and Number results from runMethod
* ✅ Generate or import private key, sign, encrypt and decrypt using Tonlib
* ✅ Encrypt/decrypt with mnemonic
* ✅ Deploy contracts and send external messages using Tonlib
* ✅ Wallets - Simple (V1), V2, V3, V4 (plugins), V5, Lockup, ~~Highload~~/Highload-V3, Highload-V3S (Secp256k1), DNS,
  Jetton/Jetton V2, StableCoin, NFT,
  Payment-channels, ~~Multisig V1~~, Multisig V2
* ✅ HashMap, HashMapE, PfxHashMap, PfxHashMapE, HashMapAug, HashMapAugE serialization / deserialization

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=ton-blockchain/ton4j&type=Date)](https://www.star-history.com/#ton-blockchain/ton4j&Date)

<!-- Badges -->

[maven-central-svg]: https://img.shields.io/maven-central/v/org.ton.ton4j/smartcontract

[maven-central]: https://mvnrepository.com/artifact/org.ton.ton4j/smartcontract

[jitpack-svg]: https://jitpack.io/v/ton-blockchain/ton4j.svg

[jitpack]: https://jitpack.io/#ton-blockchain/ton4j

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org