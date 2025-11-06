# Liteclient module

Java Lite-client wrapper uses JNA to access methods in native lite-client binary.

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>org.ton.ton4j</groupId>
    <artifactId>func</artifactId>
    <version>1.3.3</version>
</dependency>
```

## Jitpack

```xml

<dependency>
    <groupId>org.ton.ton4j</groupId>
    <artifactId>func</artifactId>
    <version>1.3.3</version>
</dependency>
```

## Usage

Notice, if you installed TON binaries using [package managers](https://github.com/ton-blockchain/packages) like brew,
apt or chocolatey you can omit specifying path to a func executable and simply use it as follows:

```java
URL resource = TestFuncRunner.class.getResource("/test.fc");
File funcFile = Paths.get(resource.toURI()).toFile();
String absolutePath = funcFile.getAbsolutePath();

FuncRunner funcRunner = FuncRunner.builder()
    .funcExecutablePath(Utils.getFuncGithubUrl())
    .build();

String result = funcRunner.run(funcFile.getParent(), "-PA", absolutePath);
log.info("output: {}", result);
```

More examples in [TestFuncRunner](../func/src/test/java/org/ton/ton4j/func/TestFuncRunner.java) class.


[maven-central-svg]: https://img.shields.io/maven-central/v/org.ton.ton4j/func

[maven-central]: https://mvnrepository.com/artifact/org.ton.ton4j/func

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org