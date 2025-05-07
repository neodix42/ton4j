# Liteclient module

Java Lite-client wrapper uses JNA to access methods in native lite-client binary.

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>fift</artifactId>
    <version>0.9.9</version>
</dependency>
```

## Jitpack

```xml

<dependency>
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>fift</artifactId>
    <version>0.9.9</version>
</dependency>
```

## Usage

Notice, if you installed TON binaries using [package managers](https://github.com/ton-blockchain/packages) like brew,
apt or chocolatey you can omit specifying path to a fift executable and simply use it as follows:

```java
FiftRunner fiftRunne=FiftRunner.builder().build();
```

```java
URL resource=TestFiftRunner.class.getResource("/test.fift");
        File fiftFile=Paths.get(resource.toURI()).toFile();
        String absolutePath=fiftFile.getAbsolutePath();

        FiftRunner fiftRunner=FiftRunner.builder().fiftExecutablePath(Utils.getFiftGithubUrl()).build();

        String result=fiftRunner.run(fiftFile.getParent(),"-s",absolutePath);
        log.info("output: {}",result);
```

More examples in [TestFiftRunner](../fift/src/test/java/org/ton/java/fift/TestFiftRunner.java) class.


[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/fift

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/fift

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org