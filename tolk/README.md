# Liteclient module

Java Lite-client wrapper uses JNA to access methods in native lite-client binary.

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>tolk</artifactId>
    <version>0.8.0</version>
</dependency>
```

## Jitpack

```xml

<dependency>
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>tolk</artifactId>
    <version>0.8.0</version>
</dependency>
```

## Usage

```java
URL resource=TestTolkRunner.class.getResource("/test.tolk");
        File tolkFile=Paths.get(resource.toURI()).toFile();
        String absolutePath=tolkFile.getAbsolutePath();

        TolkRunner tolkRunner=TolkRunner.builder().build();

        String result=tolkRunner.run(tolkFile.getParent(),absolutePath);
        log.info("output: {}",result);
```

More examples in [TestTolkRunner](../func/src/test/java/org/ton/java/tolk/TestTolkRunner.java) class.


[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/tolk

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/tolk

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org