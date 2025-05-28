# TL-B module

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>tlb</artifactId>
    <version>0.9.9</version>
</dependency>
```

## Jitpack

```xml

<dependency>
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>tlb</artifactId>
    <version>0.9.9</version>
</dependency>
```

## TL-B Serialization

```java
StorageUsed storageUsed = StorageUsed.builder()
    .bitsUsed(BigInteger.valueOf(5))
    .cellsUsed(BigInteger.valueOf(3))
    .publicCellsUsed(BigInteger.valueOf(3))
    .build();

StorageInfo storageInfo = StorageInfo.builder()
    .storageUsed(storageUsed)
    .lastPaid(1709674914)
    .duePayment(BigInteger.valueOf(12))
    .build();

Cell serializedStorageInfo = storageInfo.toCell();
```

## TL-B Deserialization

```java
Cell c = CellBuilder.beginCell().fromBoc("b5ee9c72410106010054000211b8e48dfb4a0eebb0040105022581fa7454b05a2ea2ac0fd3a2a5d348d2954008020202012004030015bfffffffbcbd0efda563d00015be000003bcb355ab466ad0001d43b9aca00250775d8011954fc40008b63e6951");
log.info("CellType {}",c.getCellType());

ValueFlow valueFlow = ValueFlow.deserialize(CellSlice.beginParse(c));
log.info("valueFlow {}",valueFlow);

//result
valueFlow ValueFlow(magic=b8e48dfb, fromPrevBlk=CurrencyCollection(coins=2280867924805872170, extraCurrencies=([239,664333333334],[4294967279,998444444446])),
toNextBlk = CurrencyCollection(coins=2280867927505872170, extraCurrencies=([239,664333333334],[4294967279,998444444446])),
imported = CurrencyCollection(coins=0, extraCurrencies=()),
exported = CurrencyCollection(coins=0, extraCurrencies=()),
feesCollected = CurrencyCollection(coins=2700000000, extraCurrencies=()),
burned=null,feesImported = CurrencyCollection(coins=1000000000, extraCurrencies=()),
recovered = CurrencyCollection(coins=2700000000, extraCurrencies=()),
created = CurrencyCollection(coins=1700000000, extraCurrencies=()),
minted = CurrencyCollection(coins=0, extraCurrencies=()))
```

[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/tlb

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/tlb

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org