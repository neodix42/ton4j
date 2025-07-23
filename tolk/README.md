# Liteclient module

Java Lite-client wrapper uses JNA to access methods in native lite-client binary.

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>tolk</artifactId>
    <version>1.0.1</version>
</dependency>
```

## Jitpack

```xml

<dependency>
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>tolk</artifactId>
    <version>1.0.1</version>
</dependency>
```

## Usage

Notice, if you installed TON binaries using [package managers](https://github.com/ton-blockchain/packages) like brew,
apt or chocolatey you can omit specifying path to a func executable and simply use it as follows:

```java
URL resource = TestTolkRunner.class.getResource("/test.tolk");
File tolkFile = Paths.get(resource.toURI()).toFile();
String absolutePath = tolkFile.getAbsolutePath();

TolkRunner tolkRunner = TolkRunner.builder().tolkExecutablePath(Utils.getTolkGithubUrl()).build();

String result=tolkRunner.run(tolkFile.getParent(),absolutePath);
log.info("output: {}",result);
```

More examples in [TestTolkRunner](../func/src/test/java/org/ton/ton4j/tolk/TestTolkRunner.java) class.


[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/tolk

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/tolk

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org