# Cell module

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>cell</artifactId>
    <version>0.4.5</version>
</dependency>
```

## Jitpack

```xml
<dependency>
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>cell</artifactId>
    <version>0.4.5</version>
</dependency>
```

## Cell Serialization to BoC

```java
Cell c1 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 25), 26).endCell();

Cell c2 = CellBuilder.beginCell()
                        .storeUint((long) Math.pow(2, 37), 38)
                        .storeRef(c1)
                     .endCell();

Cell c3 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 41), 42).endCell();
Cell c4 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 42), 44).endCell();
Cell c5 = CellBuilder.beginCell()
                        .storeAddress(Address.parseFriendlyAddress("0QAljlSWOKaYCuXTx2OCr9P08y40SC2vw3UeM1hYnI3gDY7I"))
                        .storeString("HELLO")
                        .storeRef(c2)
                      .endCell();

log.info("c1 {}", c1.bits);
log.info("c2 {}", c2.bits);
log.info("c2:\n{}", c2.print());
log.info("c3 {}", c3.bits);
log.info("c4 {}", c4.bits);
log.info("c5 {}", c5.bits);
log.info("c5:\n{}", c5.print());

byte[] serializedCell5 = c5.toBocNew();

// output
c1 8000002_
c2 8000000002_
c2:
x{8000000002_}
 x{8000002_}

c3 80000000002_
c4 40000000000
c5 8004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A908A98989F_
c5:
x{8004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A908A98989F_}
 x{8000000002_}
  x{8000002_}
```

## Cell Deserialization from BoC

```java
byte[] serializedCell5 = c5.toBocNew();
Cell dc5 = CellBuilder.beginCell().fromBoc(serializedCell5);
log.info("c5 deserialized:\n{}", dc5.print());

// output
c5 deserialized:
x{8004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A908A98989F_}
 x{8000000002_}
  x{8000002_}
```

## TLB Serialization

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

## TLB Deserialization

```java
Cell c=CellBuilder.beginCell().fromBoc("b5ee9c72410106010054000211b8e48dfb4a0eebb0040105022581fa7454b05a2ea2ac0fd3a2a5d348d2954008020202012004030015bfffffffbcbd0efda563d00015be000003bcb355ab466ad0001d43b9aca00250775d8011954fc40008b63e6951");
        log.info("CellType {}",c.getCellType());
        ValueFlow valueFlow=ValueFlow.deserialize(CellSlice.beginParse(c));
        log.info("valueFlow {}",valueFlow);

//result
        valueFlow ValueFlow(magic=b8e48dfb,fromPrevBlk=CurrencyCollection(coins=2280867924805872170,
        extraCurrencies=([239,664333333334],[4294967279,998444444446])),
        toNextBlk=CurrencyCollection(coins=2280867927505872170,extraCurrencies=([239,664333333334],[4294967279,998444444446])),
        imported=CurrencyCollection(coins=0,extraCurrencies=()),
        exported=CurrencyCollection(coins=0,extraCurrencies=()),
        feesCollected=CurrencyCollection(coins=2700000000,extraCurrencies=()),
        burned=null,feesImported=CurrencyCollection(coins=1000000000,extraCurrencies=()),
        recovered=CurrencyCollection(coins=2700000000,extraCurrencies=()),
        created=CurrencyCollection(coins=1700000000,extraCurrencies=()),
        minted=CurrencyCollection(coins=0,extraCurrencies=()))
```

More examples on how to construct [Cell](../cell/src/main/java/org/ton/java/cell/Cell.java)
and [BitString](../bitstring/src/main/java/org/ton/java/bitstring/BitString.java) can be
found in [TestCell](../cell/src/test/java/org/ton/java/cell/TestCell.java)
, [TestCellBuilder](../cell/src/test/java/org/ton/java/cell/TestCellBuilder.java)
, [TestHashMap](../cell/src/test/java/org/ton/java/cell/TestHashMap.java)
and [TestBitString](../bitstring/src/test/java/org/ton/java/bitstring/TestBitString.java) classes.


[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/cell

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/cell

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org