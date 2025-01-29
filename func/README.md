# Liteclient module

Java Lite-client wrapper uses JNA to access methods in native lite-client binary.

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>func</artifactId>
    <version>0.9.1</version>
</dependency>
```

## Jitpack

```xml

<dependency>
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>func</artifactId>
    <version>0.9.1</version>
</dependency>
```

## Usage

Notice, if you installed TON binaries using [package managers](https://github.com/ton-blockchain/packages) like brew,
apt or chocolatey you can omit specifying path to a func executable and simply use it as follows:

```java
FuncRunner funcRunner=FuncRunner.builder().build();
```

```java
        URL resource=TestFuncRunner.class.getResource("/test.fc");
        File funcFile=Paths.get(resource.toURI()).toFile();
        String absolutePath=funcFile.getAbsolutePath();

        FuncRunner funcRunner=FuncRunner.builder().funcExecutablePath(resourcePath).build();

        String result=funcRunner.run(funcFile.getParent(),"-PA",absolutePath);
        log.info("output: {}",result);
```

More examples in [TestFuncRunner](../func/src/test/java/org/ton/java/func/TestFuncRunner.java) class.


[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/func

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/func

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org