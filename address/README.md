# Address module

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml
<dependency>
    <groupId>org.ton.ton4j</groupId>
    <artifactId>address</artifactId>
    <version>1.3.4</version>
</dependency>
```

## Jitpack

```xml

<dependency>
    <groupId>org.ton.ton4j</groupId>
    <artifactId>address</artifactId>
    <version>1.3.4</version>
</dependency>
```

## Formatting

Just like in TonWeb toString() method has some arguments that help to format final address:

```java
public class Address {
    public String toString(boolean isUserFriendly,
                           boolean isUrlSafe,
                           boolean isBounceable,
                           boolean isTestOnly);
```

```java
Address address01=Address.of("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
assertThat(address01.toString()).isEqualTo("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");

Address address02=Address.of("kQAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi47nL");
assertThat(address02.toString()).isEqualTo("kQAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi47nL");
assertThat(address02.isBounceable).isEqualTo(true);

Address address03=Address.of("0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3");
assertThat(address03.toString()).isEqualTo("0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3");

Address address04=Address.of("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
assertThat(address04.toString(true,true,false)).isEqualTo("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
assertThat(address04.isBounceable).isFalse();

Address address05=Address.of("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
assertThat(address05.toString(true,true,true)).isEqualTo("kQAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi47nL");
assertThat(address05.isBounceable).isFalse();

Address address06=Address.of("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
assertThat(address06.toString(false)).isEqualTo("0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3");
assertThat(address06.isBounceable).isFalse();

Address address07=Address.of("kQAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi47nL");
assertThat(address07.toString(true,true,false)).isEqualTo("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
assertThat(address07.isBounceable).isTrue();
```

More examples on how to construct [Cell](../cell/src/main/java/org/ton/ton4j/cell/Cell.java)
and [BitString](../bitstring/src/main/java/org/ton/ton4j/bitstring/BitString.java) can be
found in [TestCell](../cell/src/test/java/org/ton/ton4j/cell/TestCell.java)
and [TestBitString](../bitstring/src/test/java/org/ton/ton4j/bitstring/TestBitString.java) classes.

[maven-central-svg]: https://img.shields.io/maven-central/v/org.ton.ton4j/address

[maven-central]: https://mvnrepository.com/artifact/org.ton.ton4j/address

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org