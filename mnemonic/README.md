# Mnemonic module

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml
<dependency>
    <groupId>com.github.neodix42</groupId>
    <artifactId>mnemonic</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Jitpack

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
    <groupId>com.github.neodiX42.ton-java</groupId>
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

More examples on how to construct [Mnemonic](../mnemonic/src/main/java/org/ton/java/mnemonic/Mnemonic.java) can be
found in [TestMnemonic](../mnemonic/src/test/java/org/ton/java/mnemonic/TestMnemonic.java) class.

[maven-central-svg]: https://img.shields.io/maven-central/v/org.ton.java/mnemonic

[maven-central]: https://mvnrepository.com/artifact/org.ton.java/mnemonic

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org