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
    <version>0.6.0</version>
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
    <version>0.6.0</version>
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
* [Fift](fift/README.md) - wrapper for using with external precompiled fift binary.
* [Func](func/README.md) - wrapper for using with external precompiled func binary.
* [TonConnect](tonconnect/README.md) - implementation of Ton Connect standard.
* [Utils](utils/README.md) - create private and public keys, convert data, etc.

### Features

* ✅ BitString manipulations
* ✅ Cells serialization / deserialization
* ✅ TL-B serialization / deserialization
* ✅ Cell builder and cell slicer (reader)
* ✅ Tonlib wrapper
* ✅ Lite-client wrapper
* ✅ Support num, cell and slice as arguments for runMethod
* ✅ Render List, Tuple, Slice, Cell and Number results from runMethod
* ✅ Generate or import private key, sign, encrypt and decrypt using Tonlib
* ✅ Encrypt/decrypt with mnemonic
* ✅ Send external message
* ✅ Get block transactions
* ✅ Deploy contracts and send external messages using Tonlib
* ✅ Wallets - Simple (V1), V2, V3, V4 (plugins), Lockup, Highload/Highload-V3, DNS, Jetton, StableCoin, NFT,
  Payment-channels,
  Multisig V1
* ✅ HashMap, HashMapE, PfxHashMap, PfxHashMapE, HashMapAug, HashMapAugE serialization / deserialization

## Support ton4j development
If you want to speed up ton4j development and thus change its priority in my backlog, you are welcome to donate some toncoins:

```UQBguBMWc_wUA8pJjC-A9JbTJFzb7lbFbbkiFYajA33-U9YU```

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=neodiX42/ton4j&type=Date)](https://star-history.com/#neodiX42/ton4j&Date)

<!-- Badges -->

[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/smartcontract

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/smartcontract

[jitpack-svg]: https://jitpack.io/v/neodiX42/ton4j.svg

[jitpack]: https://jitpack.io/#neodiX42/ton4j

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org