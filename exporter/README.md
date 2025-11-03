# Exporter module

Java Exporter module uses RocksDB JNI library to directly access and extract data from the TON database located locally on your host.  

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>io.github.neodix42</groupId>
    <artifactId>exporter</artifactId>
    <version>1.3.2</version>
</dependency>
```

## Jitpack

```xml

<dependency>
    <groupId>io.github.neodix42.ton4j</groupId>
    <artifactId>exporter</artifactId>
    <version>1.3.2</version>
</dependency>
```

# Exporter

Mainly `Exporter` class suggests following methods:
 - `exportToFile()` - used to export blocks of all shards in JSON or BoC (hex format) to file. 
 - `exportToStdout()` - used to export blocks in JSON or BoC (hex format) to Stdout.
 - `exportToObjects()` - used to get access to parallel stream of all blocks of TL-B type [Block](https://github.com/ton-blockchain/ton/blob/master/crypto/block/block.tlb). 
 - `getLast()` and `getLast(X)` - used to get the latest block or list of last blocks limited by X.
 - `getLast(wc,shard)` - used to get the latest block by wc and shard.
 - `getBlock(BlockIdExt)` - used to get a Block of TL-B type by seqno, workchain, shard, root and file hashes. 
 - `getBlock(BlockId)` - used to get a Block of TL-B type by seqno, workchain and shard. 
 - `getBlockIdExt(BlockId)` - used to get a BlockIdExt of TL-B type by seqno, workchain and shard. 
 - `getShardAccountByAddress(...)` - used to get a ShardAccount of TL-B type by seqno, workchain and shard. 
 - `getBalance(Address)` - used to get account's balance. 
 - `getBalance(Address, long)` - used to get account's balance by address and masterchain seqno. 


First three methods have parameters:
 - deserialized - true or false. If `true` TL-B object of type Block in JSON format will be stored per line, otherwise only BoC in hex format.
 - number of threads
 - support resume functionality.

**Important**

Currently export to JSON is straight forward in terms that same Cells (BoCs) are duplicated accross the export file.
This makes block in JSON format much bigger than its original BoC representation.
For example, I found one block which BoC was of 1MB in size and its JSON turned to 200MB (there were about 1000 Txs where each Tx had the same InitState (code+body)).

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

## Predefined TON RockDB [readers](src/main/java/org/ton/ton4j/exporter/reader)

### GlobalIndexDbReader
Specialized [reader](org/ton/ton4j/exporter/reader/GlobalIndexDbReader.java) for TON **Files** database global index. 
This reader focuses specifically on the Global index at `db/files/globalindex`. 
TON **Files** database uses: 
1. Main index entry
   * key = db.files.index.key (empty)
   * value = db.files.index.value (package lists)
2. Package metadata entries
   * key = db.files.package.key
   * value = db.files.package.value    
3. File hash entries 
   * key = file hash (raw bytes)
   * value = location data (package_id, offset, size)

### ArchiveIndexReader
[Reader](org/ton/ton4j/exporter/reader/ArchiveIndexReader.java) for individual archive index databases (archive.XXXXX.index). 
Each archive package has a corresponding RocksDB index that contains hash-&gt;offset mappings for files within that package.

The archive index database contains several special keys:

1. `status` - Contains the value `"sliced"` to indicate the archive uses sliced storage.
2. `slices` - Stores the total number of slices (as a string integer)
3. `slice_size` - Stores the size of each slice
4. `info.{i}` - Stores the raw package filename for slice index `i` (without `.pack` extension)

### StateDbReader
Specialized [reader](org/ton/ton4j/exporter/reader/StateDbReader.java) for TON archive state database. Handles files stored in the `archive/states` directory.
Mostly used to get latest BlockIdExt from the TON state files. 

### CellDbReader
[Reader](org/ton/ton4j/exporter/reader/CellDbReader.java) for TON **Cell** database. The CellDB stores blockchain state cells in a RocksDB database with a linked-list structure for metadata entries.

Based on the original TON C++ implementation in celldb.cpp, the CellDB uses: 
1. Metadata entries: 
   * key = "desc" + SHA256(TL-serialized block_id) 
   * value = TL-serialized db.celldb.value 
2.Special empty entry: 
   * key = "desczero", 
   * value = TL-serialized db.celldb.value 
3. Cell data entries: 
   * key = cell hash (raw bytes)
   * value = serialized cell content 
4. Linked list structure entries connected via prev/next pointers forming a doubly-linked list.

The TL types used:
   * db.celldb.key.value: hash:int256
   * db.celldb.value: block_id:tonNode.blockIdExt prev:int256 next:int256 root_hash:int256 
   

### TempPackageIndexReader
[Reader](org/ton/ton4j/exporter/reader/TempPackageIndexReader.java) for **Temp** package index databases. Each temp package (e.g., temp.archive.1756843200.pack) has a corresponding RocksDB index (temp.archive.1756843200.index) that contains hash-&gt;offset mappings for fast block lookup.

Based on the original C++ implementation in archive-db.cpp, temp package indexes store:
1. File hash entries: 
   * key = file hash (hex string)
   * value = offset (8 bytes, little-endian)  
2. Status entry: 
   * key = "status"
   * value = package size (8 bytes, little-endian)

### PackageReader
[Used](org/ton/ton4j/exporter/reader/PackageReader.java) to parse package files (like archive.00000.pack) where each package file starts with package header `0xae8fdd01` 
and each entry starts with entry header `0x1e8b` and follows by `filenameLength` (2 bytes), `dataSize` (4 bytes), `fileName`, `data`. 
Where `data` is BoC as array of bytes.
## Usage of TonExporterApp

`TonExporterApp.jar` is a standalone uber-jar application, that allows you to run exports of TON DB on your local host.  

```java
java -jar target/TonExporterApp.jar 
Usage:
  For version: java -jar TonExporterApp -v
  For file output: java -jar TonExporterApp.jar <ton-db-root-path> file <json|boc> <num-of-threads> <true|false> <output-file-name> [last]
  For stdout output: java -jar TonExporterApp.jar <ton-db-root-path> stdout <json|boc> <num-of-threads> [<true|false>] [last]
  For balance query: java -jar TonExporterApp.jar <ton-db-root-path> balance <address> [seqno]

Arguments:
  -v                : Show version information
  ton-db-root-path  : Path to the TON database root directory
  file|stdout       : Output destination (file or stdout)
  json|boc         : Output format (json for deserialized, boc for raw hex)
  num-of-threads   : Number of parallel threads to use
  true|false       : Whether to show progress information during export
  output-file-name : Name of the output file (required only for file output)
  last             : Optional flag to get only the last block
  balance          : Query account balance
  address          : TON address in string format (required for balance)
  seqno            : Block sequence number (optional for balance)                        

Last Mode:
  When 'last' is specified as the final argument:
  - Progress display is ignored and set to false
  - Number of threads is ignored and set to 1
  - Only the most recent block is retrieved and output
  - For stdout + json: additional info is shown (seqno, transactions count, messages count)

Balance Mode:
  Query account balance from the TON database:
  - Without seqno: returns current balance
  - With seqno: returns balance at specified masterchain block
  - Balance is printed to stdout as a number

Examples:
  java -jar TonExporterApp.jar -v
  java -jar TonExporterApp.jar /var/ton-work/db file json 4 true blocks.json
  java -jar TonExporterApp.jar /var/ton-work/db stdout boc 8
  java -jar TonExporterApp.jar /var/ton-work/db file json 1 false last_block.json last
  java -jar TonExporterApp.jar /var/ton-work/db stdout json 1 last
  java -jar TonExporterApp.jar /var/ton-work/db balance EQD...
  java -jar TonExporterApp.jar /var/ton-work/db balance EQD... 12345678

```

More examples in [Exporter](../exporter/src/test/java/org/ton/ton4j/exporter) module.

[maven-central-svg]: https://img.shields.io/maven-central/v/io.github.neodix42/emulator

[maven-central]: https://mvnrepository.com/artifact/io.github.neodix42/emulator

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org