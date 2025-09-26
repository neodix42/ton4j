# Exporter module

Java Exporter module uses RocksDB JNI library to directly access and extract data from the TON database located locally on your host.  

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>exporter</artifactId>
    <version>1.1.1</version>
</dependency>
```

## Jitpack

```xml

<dependency>
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>exporter</artifactId>
    <version>1.1.1</version>
</dependency>
```

# Exporter

Mainly `Exporter` class suggests three methods:
 - `exportToFile()` - used to export blocks of all shards to file. 
 - `exportToStdout()` - used to export blocks in JSON or BoC (hex format) to Stdout.
 - `exportToObjects()` - used to get access to parallel stream of all blocks of TL-B type [Block](https://github.com/ton-blockchain/ton/blob/master/crypto/block/block.tlb). 
 - `getLast()` and `getLast(X)` - used to get very last block or list of last blocks limited by X.


First three methods have parameters:
 - deserialized - true or false. If `true` TL-B object of type Block in JSON format will be stored per line, otherwise only BoC in hex format.
 - number of threads 

All methods and `TonExporterApp` support resume functionality.

**Important**

Currently export to JSON is straight forward in terms that same Cells (BoCs) are duplicated accross the export file.
This makes block in JSON format much bigger than its original BoC representation.
For example, I found one block which BoC was 1MB size and its JSON turned to 200MB (there were about 1000 Txs where each Tx had same InitState (code+body)).

In the future, this might be optmized so export would produce a sepate file where each cell would be referenced by its hash, and that hash would be used in block as a reference.

## Usage of Exporter

The simplest way to fetch all blocks of TON database from Java is to use Exporter's `exportToObjects()` method.
This way you will get a parallel stream that contains all deserilized blocks.

```java
Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();

Stream<ExportedBlock> blockStream = exporter.exportToObjects(true, 20);

blockStream.forEach(
    block -> {
        log.info(
          "Block info - Workchain: {}, Shard: {}, Seqno: {}",
          block.getWorkchain(),
          block.getShard(),
          block.getSeqno());
});
```
### Get last deserialized block

```java
Exporter exporter = Exporter.builder().tonDatabaseRootPath(TON_DB_ROOT_PATH).build();
Block lastBlock = exporter.getLast();
```

## Generic RockDB access
For that use `RocksDbWrapper`. Below are few examples of how to traverse any RockDB key-value database.

Cell DB traversal
```java
  @Test
  public void testCellDbReader() throws IOException {
    RocksDbWrapper cellDb = new RocksDbWrapper(TON_ROOT_DB_PATH + "/celldb");
    cellDb.forEach(
        (key, value) -> {
          log.info("key:{}, value:{}", Utils.bytesToHex(key), Utils.bytesToHex(value));
        });
    cellDb.close();
  }
```

Global Index DB traversal
```java
  @Test
public void testGlobalIndexReading() throws IOException {
    try (RocksDbWrapper reader = new RocksDbWrapper(TON_ROOT_DB_PATH + "/files/globalindex")) {
        reader.forEach((key, value) -> {
            log.info(
                "globalIndexKey: {}, globalIndexValue: {}",
                GlobalIndexKey.deserialize(key),
                GlobalIndexValue.deserialize(value));
            });
    }
}
```

## Usage of TonExporterApp

`TonExporterApp.jar` is a standalone uber-jar application, that allows you to run exports of TON DB on your local host.  

```java
java -jar target/TonExporterApp.jar 
Usage:
  For version: java -jar TonExporterApp -v
  For file output: java -jar TonExporterApp.jar <ton-db-root-path> file <json|boc> <num-of-threads> <true|false> <output-file-name> [last]
  For stdout output: java -jar TonExporterApp.jar <ton-db-root-path> stdout <json|boc> <num-of-threads> [<true|false>] [last]

Arguments:
  -v                : Show version information
  ton-db-root-path  : Path to the TON database root directory
  file|stdout       : Output destination (file or stdout)
  json|boc         : Output format (json for deserialized, boc for raw hex)
  num-of-threads   : Number of parallel threads to use
  true|false       : Whether to show progress information during export
  output-file-name : Name of the output file (required only for file output)
  last             : Optional flag to get only the last block

Last Mode:
  When 'last' is specified as the final argument:
  - Progress display is ignored and set to false
  - Number of threads is ignored and set to 1
  - Only the most recent block is retrieved and output
  - For stdout + json: additional info is shown (seqno, transactions count, messages count)

Examples:
  java -jar TonExporterApp.jar -v
  java -jar TonExporterApp.jar /var/ton-work/db file json 4 true blocks.json
  java -jar TonExporterApp.jar /var/ton-work/db stdout boc 8
  java -jar TonExporterApp.jar /var/ton-work/db file json 1 false last_block.json last
  java -jar TonExporterApp.jar /var/ton-work/db stdout json 1 last
```

More examples in [Exporter](../exporter/src/test/java/org/ton/ton4j/exporter) module.

[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/emulator

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/emulator

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org