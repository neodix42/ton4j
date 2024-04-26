# Liteclient module

Java Lite-client wrapper uses JNA to access methods in native lite-client binary.

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml
<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>lite-client</artifactId>
    <version>0.3.0</version>
</dependency>
```

## Jitpack

```xml
<dependency>
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>lite-client</artifactId>
    <version>0.3.0</version>
</dependency>
```

## Simply usage

```java
LiteClient liteClient = LiteClient.builder()
    .pathToLiteClientBinary(pathToLiteClient)
    .testnet(true)
    .pathToGlobalConfig(pathToGlobalConfig)
    .build();

String stdout = liteClient.executeLast();

```

More examples in [LiteClientTest](../liteclient/src/test/java/org/ton/java/liteclient/LiteClientTest.java) class.


[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/tonlib

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/tonlib

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org