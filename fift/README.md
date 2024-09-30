# Liteclient module

Java Lite-client wrapper uses JNA to access methods in native lite-client binary.

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml
<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>fift</artifactId>
    <version>0.7.0</version>
</dependency>
```

## Jitpack

```xml
<dependency>
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>fift</artifactId>
    <version>0.7.0</version>
</dependency>
```

## Usage

```java
URL resource=TestFiftRunner.class.getResource("/test.fift");
        File fiftFile=Paths.get(resource.toURI()).toFile();
        String absolutePath=fiftFile.getAbsolutePath();

        FiftRunner fiftRunner=FiftRunner.builder().build();

        String result=fiftRunner.run(fiftFile.getParent(),"-s",absolutePath);
        log.info("output: {}",result);
```

More examples in [TestFiftRunner](../fift/src/test/java/org/ton/java/fift/TestFiftRunner.java) class.


[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/tonlib

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/tonlib

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org