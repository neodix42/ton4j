# Mnemonic module

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml
<dependency>
    <groupId>org.ton.java</groupId>
    <artifactId>mnemonic</artifactId>
    <version>0.0.1</version>
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
    <groupId>com.github.neodiX42.ton-java-temp1</groupId>
    <artifactId>mnemonic</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Usage example

```java
List<String> mnemonic = Mnemonic.generate(24, pwd);
String mnemonicPhrase = String.join(" ", mnemonic);

//result
<<mnemonicPhrase containing 24 random words>>
```

More examples on how to work with `Mnemonic` can be found in `TestMnemonic` class.


[maven-central-svg]: https://img.shields.io/maven-central/v/org.ton.java/mnemonic
[maven-central]: https://mvnrepository.com/artifact/org.ton.java/mnemonic
[jitpack-svg]: https://jitpack.io/v/neodiX42/ton-java.svg
[jitpack]: https://jitpack.io/#neodiX42/ton-java/mnemonic
[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue
[ton]: https://ton.org