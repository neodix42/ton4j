# Liteclient module

Java Lite-client wrapper uses JNA to access methods in native lite-client binary.

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>lite-client</artifactId>
    <version>0.9.6</version>
</dependency>
```

## Jitpack

```xml

<dependency>
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>lite-client</artifactId>
    <version>0.9.6</version>
</dependency>
```

## Usage

```java
LiteClient liteClient=LiteClient.builder()
        .pathToLiteClientBinary(Utils.getLiteClientGithubUrl())
        .testnet(true)
        .build();

        String stdout=liteClient.executeLast();

```

More examples in [LiteClientTest](../liteclient/src/test/java/org/ton/java/liteclient/LiteClientTest.java) class.


[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/liteclient

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/liteclient

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org