# Emulator module

Java Emulator wrapper uses JNA to access methods in native emulator shared library.

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>emulator</artifactId>
    <version>0.9.1</version>
</dependency>
```

## Jitpack

```xml

<dependency>
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>emulator</artifactId>
    <version>0.9.1</version>
</dependency>
```

## Usage

```java
Tonlib tonlib=Tonlib.builder().build(); // we need tonlib to get network config
        Cell config=tonlib.getConfigAll(128);
        TxEmulator txEmulator=TxEmulator.builder()
        .configBoc(config.toBase64())
        .build();

        SmcLibraryResult result=tonlib.getLibraries(
        List.of("wkUmK4wrzl6fzSPKM04dVfqW1M5pqigX3tcXzvy6P3M="));
        log.info("result: {}",result);

        TonHashMapE x=new TonHashMapE(256);

        for(SmcLibraryEntry l:result.getResult()){
        String cellLibBoc=l.getData();
        Cell lib=Cell.fromBocBase64(cellLibBoc);
        log.info("cell lib {}",lib.toHex());
        x.elements.put(1L,lib);
        x.elements.put(2L,lib); // 2nd because of the bug in hashmap/e
        }

        Cell dictLibs=x.serialize(
        k->CellBuilder.beginCell().storeUint((Long)k,256).endCell().getBits(),
        v->CellBuilder.beginCell().storeRef((Cell)v)
        );

        log.info("txEmulator.setLibs() result {}",txEmulator.setLibs(dictLibs.toBase64()));
```

More examples in [TestTxEmualtor](../emulator/src/test/java/org/ton/java/emulator/TestTxEmulator.java) class.


[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/emulator

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/emulator

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org