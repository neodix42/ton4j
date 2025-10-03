package org.ton.ton4j.exporter.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.exporter.types.ArchiveFileLocation;
import org.ton.ton4j.tl.types.db.block.BlockIdExt;
import org.ton.ton4j.tl.types.db.block.BlockInfo;
import org.ton.ton4j.tl.types.db.blockdb.key.BlockDbValueKey;
import org.ton.ton4j.tl.types.db.filedb.key.BlockFileKey;
import org.ton.ton4j.tl.types.db.files.GlobalIndexKey;
import org.ton.ton4j.tl.types.db.files.GlobalIndexValue;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.utils.Utils;

/**
 * Test class for the FilesDbReader that demonstrates the optimized block reading approach using the
 * Files database global index.
 */
@Slf4j
public class TestGlobalIndexDbReader {

  BlockIdExt blockIdExtMc =
      BlockIdExt.builder()
          .workchain(-1)
          .seqno(229441)
          .shard(0x8000000000000000L)
          .rootHash(
              Utils.hexToSignedBytes(
                  "439233F8D4B99BAD7A2CC84FFE0D16150ADC0E1058BCDF82243D1445A75CA5BF"))
          .fileHash(
              Utils.hexToSignedBytes(
                  "E24EA0E5F520135DA4FC0B0477E5440E0D1C4E7EDB2026941F0457376BB3D97E"))
          .build();

  BlockIdExt blockIdExt =
      BlockIdExt.builder()
          .workchain(0)
          .seqno(229441)
          .shard(0x8000000000000000L)
          .rootHash(
              Utils.hexToSignedBytes(
                  "5F49521AD8EC570C82B6DA6D1AF9D16884CA17F3310044BBB66ED6B94A15608C"))
          .fileHash(
              Utils.hexToSignedBytes(
                  "7925B49AF1FF46550998947C05EC2B2AAD2F89B1C4FA98F3A19DDB62ACDF36EC"))
          .build();

  private static final String TON_DB_ROOT_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  public static String mcBlock =
      "(-1,8000000000000000,229441):439233F8D4B99BAD7A2CC84FFE0D16150ADC0E1058BCDF82243D1445A75CA5BF:E24EA0E5F520135DA4FC0B0477E5440E0D1C4E7EDB2026941F0457376BB3D97E";
  public static String block =
      "(0,8000000000000000,229441):5F49521AD8EC570C82B6DA6D1AF9D16884CA17F3310044BBB66ED6B94A15608C:7925B49AF1FF46550998947C05EC2B2AAD2F89B1C4FA98F3A19DDB62ACDF36EC";

  @Test
  public void testGetOffsetByHash() throws IOException {

    BlockFileKey blockFileKey = BlockFileKey.builder().blockIdExt(blockIdExt).build();

    // BlockExtId hash
    String keyHash = "C71E95AB52F673185AD62AC52F3DFEAFE2CB2DEA12B802061F770FDA498E2E97";

    int archiveIndex;
    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {
      archiveIndex =
          reader.getArchiveIndexBySeqno(blockIdExt.getWorkchain(), blockIdExt.getSeqno());
      log.info("archiveIndex: {}", archiveIndex); // index is correct
    }

    try (ArchiveIndexReader archiveIndexReader =
        new ArchiveIndexReader(TON_DB_ROOT_PATH, archiveIndex)) {
      long offset = archiveIndexReader.getOffsetByHash(blockFileKey.getKeyHash());
      log.info("offset: {}", offset);
    }
  }

  @Test
  public void testGetNonMasterchainBlock() throws IOException {
    //    int mcSeqno = 234048;

    BlockFileKey blockFileKey = BlockFileKey.builder().blockIdExt(blockIdExt).build();
    //    String keyHash = "C71E95AB52F673185AD62AC52F3DFEAFE2CB2DEA12B802061F770FDA498E2E97";

    int archiveIndex;
    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {
      archiveIndex =
          reader.getArchiveIndexBySeqno(blockIdExt.getWorkchain(), blockIdExt.getSeqno());
      log.info("found archiveIndex: {}", archiveIndex); // index is correct
    }

    long offset;
    try (ArchiveIndexReader archiveIndexReader =
        new ArchiveIndexReader(TON_DB_ROOT_PATH, archiveIndex)) {
      offset = archiveIndexReader.getOffsetByHash(blockFileKey.getKeyHash());
      log.info("found offset: {}", offset);
      BlockDbValueKey key = BlockDbValueKey.builder().blockIdExt(blockIdExt).build();
      BlockInfo blockInfo = archiveIndexReader.getDbInfoByHash(key.getKeyHash());
      log.info("found masterchainRefSeqno: {}", blockInfo.getMasterRefSeqno());

      String packFilename =
          archiveIndexReader.getExactPackFilename(
              archiveIndex,
              blockIdExt.getSeqno(),
              blockIdExt.getWorkchain(),
              blockIdExt.getShard(),
              blockInfo.getMasterRefSeqno());
      log.info("found pack filename: {}", packFilename);
      try (PackageReader packageReader = new PackageReader(packFilename)) {
        PackageReader.PackageEntry packageEntry = packageReader.getEntryAt(offset);
        Block block = packageEntry.getBlock();
        log.info("block: {}", block);
      }
    }
  }

  @Test
  public void testGetMasterchainBlock() throws IOException {

    BlockFileKey keyHash = BlockFileKey.builder().blockIdExt(blockIdExtMc).build();
    log.info("keyHash: {}", keyHash.getKeyHash());
    //    String keyHash = "827905DC8B797D1E80EFF7CACB2A5606A30F90C91C2A74CDBAF306594E7C8813";

    int archiveIndex;
    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {
      archiveIndex =
          reader.getArchiveIndexBySeqno(blockIdExtMc.getWorkchain(), blockIdExtMc.getSeqno());
      log.info("found archiveIndex: {}", archiveIndex);
    }

    long offset;
    try (ArchiveIndexReader archiveIndexReader =
        new ArchiveIndexReader(TON_DB_ROOT_PATH, archiveIndex)) {
      offset = archiveIndexReader.getOffsetByHash(keyHash.getKeyHash());
      log.info("found offset: {}", offset);

      String packFilename =
          archiveIndexReader.getExactPackFilename(
              archiveIndex,
              blockIdExtMc.getSeqno(),
              blockIdExtMc.getWorkchain(),
              blockIdExtMc.getShard(),
              blockIdExtMc.getSeqno());
      log.info("found pack filename: {}", packFilename);
      try (PackageReader packageReader = new PackageReader(packFilename)) {
        PackageReader.PackageEntry packageEntry = packageReader.getEntryAt(offset);
        Block block = packageEntry.getBlock();
        log.info("block: {}", block);
      }
    }
  }

  @Test
  public void testGetMasterchainBlockHandle() throws IOException {

    BlockDbValueKey keyHash = BlockDbValueKey.builder().blockIdExt(blockIdExtMc).build();

    int archiveIndex;
    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {
      archiveIndex =
          reader.getArchiveIndexBySeqno(blockIdExtMc.getWorkchain(), blockIdExtMc.getSeqno());
      log.info("found archiveIndex: {}", archiveIndex);
    }

    try (ArchiveIndexReader archiveIndexReader =
        new ArchiveIndexReader(TON_DB_ROOT_PATH, archiveIndex)) {
      BlockInfo block = archiveIndexReader.getDbInfoByHash(keyHash.getKeyHash());
      log.info("found BlockInfo: {}", block);
      log.info("found masterRefSeqno: {}", block.getMasterRefSeqno());
    }
  }

  @Test
  public void testGetNonMasterchainBlockHandle() throws IOException {
    BlockDbValueKey keyHash = BlockDbValueKey.builder().blockIdExt(blockIdExt).build();

    int archiveIndex;
    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {
      archiveIndex =
          reader.getArchiveIndexBySeqno(blockIdExt.getWorkchain(), blockIdExt.getSeqno());
      log.info("found archiveIndex: {}", archiveIndex);
    }

    try (ArchiveIndexReader archiveIndexReader =
        new ArchiveIndexReader(TON_DB_ROOT_PATH, archiveIndex)) {
      BlockInfo block = archiveIndexReader.getDbInfoByHash(keyHash.getKeyHash());
      log.info("found BlockInfo: {}", block);
      log.info("found masterRefSeqno: {}", block.getMasterRefSeqno());
    }
  }

  @Test
  public void testReadOffsetsOfAllIndexes() throws IOException {
    BlockDbValueKey keyHash = BlockDbValueKey.builder().blockIdExt(blockIdExtMc).build();
    // BlockExtId hash
    //    String keyHash = "267363F4522711EDF2EE27B45E94592467BEC5BECD4FEBBC250A000D0E479E6A";
    //    String keyHash = "C71E95AB52F673185AD62AC52F3DFEAFE2CB2DEA12B802061F770FDA498E2E97";

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {
      reader
          .getAllArchiveIndexesIds()
          .forEach(
              id -> {
                try (ArchiveIndexReader archiveIndexReader =
                    new ArchiveIndexReader(TON_DB_ROOT_PATH, id)) {

                  byte[] val = archiveIndexReader.getIndexDb().get(keyHash.getKeyHash());

                  if (val != null) {
                    int v = Utils.bytesToIntX(val);
                    log.info(
                        "hoorray {}, offset {}",
                        archiveIndexReader.getArchiveIndexPath(),
                        new String(val)); // 858995001, string 1893399
                  }

                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }
  }

  @Test
  public void testGlobalIndexReading() throws IOException {
    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {
      reader
          .getGlobalIndexDb()
          .forEach(
              (key, value) -> {
                log.info(
                    "globalIndexKey: {}, globalIndexValue: {}",
                    GlobalIndexKey.deserialize(key),
                    GlobalIndexValue.deserialize(value));
              });
    }
  }

  @Test
  public void testGlobalIndexReadingFindPackageBySeqno() throws IOException {
    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {
      int packageId = reader.getArchiveIndexBySeqno(0, 229441);
      log.info("packageId: {}", packageId); // index is correct
    }
  }

  @Test
  public void testArchiveIndexReaderGetExactPackFilename() throws IOException {
    int wc = 0;
    int seqno = 229441;
    int mcSeqno = 234048; // "minRefMcSeqno": 234048,
    long shard = 0x8000000000000000L;

    int packageId = 228715;

    String archiveIndexPath = ArchiveIndexReader.getArchiveIndexPath(TON_DB_ROOT_PATH, packageId);
    try (ArchiveIndexReader archiveIndexReader = new ArchiveIndexReader(archiveIndexPath)) {
      String packFilename =
          archiveIndexReader.getExactPackFilename(packageId, seqno, wc, shard, mcSeqno);
      log.info("packFilename: {}", packFilename);
      // for wc -1, seqno 229441 :
      // arch0002/archive.229415.pack
      // for wc 0, seqno 229441 based on mcSeqno = 234048
      // archive.234015.0:8000000000000000.pack
    }
  }

  @Test
  public void testBlockFileKeyGeneration() {

    // in file arch0002/archive.229415.pack
    //
    // actual file pos - 669037, (exists offset 669036) with hash
    // 353DB2CEF7A9E86310D975713D894D808B4AC1091D1E1CD572AEC5D116175C02

    // actual file pos 679194, (exists offset 679190) - with hash
    // 827905DC8B797D1E80EFF7CACB2A5606A30F90C91C2A74CDBAF306594E7C8813
    // datasize 9985

    String blockFileKeyMagic =
        "db.filedb.key.blockFile block_id:tonNode.blockIdExt = db.filedb.Key";
    String blockValueKeyMagic =
        "db.block.archivedInfo id:tonNode.blockIdExt flags:# next:flags.0?tonNode.blockIdExt = db.block.Info";
    //    String blockIdExtMagic =
    //        "tonNode.blockIdExt workchain:int shard:long seqno:int root_hash:int256
    // file_hash:int256 = tonNode.BlockIdExt";

    int l1 = (int) Utils.getQueryCrc32IEEEE(blockValueKeyMagic);
    //    int l2 = (int) Utils.getQueryCrc32IEEEE(blockIdExtMagic);

    log.info("db.block.packedInfo: {} 0x{}", l1, Integer.toHexString(l1));

    // db.block.packedInfo: 1186697618 0x46bb9192
    // db.block.archivedInfo: db.block.packedInfo: 543128145 0x205f7a51
    BlockFileKey blockFileKey = BlockFileKey.builder().blockIdExt(blockIdExt).build();
    BlockFileKey blockFileKeyMc = BlockFileKey.builder().blockIdExt(blockIdExtMc).build();

    log.info(
        "serialized blockFileKey: {}", Utils.bytesToHex(blockFileKey.serialize()).toUpperCase());
    String hexKey = Utils.bytesToHex(Utils.sha256AsArray(blockFileKey.serialize())).toUpperCase();
    String hexKeyMc =
        Utils.bytesToHex(Utils.sha256AsArray(blockFileKeyMc.serialize())).toUpperCase();

    log.info("keyHash: {}", hexKey);
    log.info("keyHashMc: {}", hexKeyMc);
    // FD848735B1D8C84AF500560E2C7CFFD46FC7EC679C6213B284F2061F0B8A2025
    // 8CFFFB4F8FAFD15046FCFC2BA01162AADB322B276AEF89E24D7F16347EAE65C4

    // correct hash for wc=0, seqno=229441
    // C71E95AB52F673185AD62AC52F3DFEAFE2CB2DEA12B802061F770FDA498E2E97
    // correct hash for wc-1, seqno=229441
    // 827905DC8B797D1E80EFF7CACB2A5606A30F90C91C2A74CDBAF306594E7C8813

  }

  // 71E4EAB0000000000000000000000080418003005F49521AD8EC570C82B6DA6D1AF9D16884CA17F3310044BBB66ED6B94A15608C7925B49AF1FF46550998947C05EC2B2AAD2F89B1C4FA98F3A19DDB62ACDF36EC00000000
  // 71E4EAB0000000000000000000000080418003005F49521AD8EC570C82B6DA6D1AF9D16884CA17F3310044BBB66ED6B94A15608C7925B49AF1FF46550998947C05EC2B2AAD2F89B1C4FA98F3A19DDB62ACDF36EC

  @Test
  public void testArchiveIndexReaderGetAllPackFiles() throws IOException {
    int archiveIndex = 228715;
    String archiveIndexPath =
        ArchiveIndexReader.getArchiveIndexPath(TON_DB_ROOT_PATH, archiveIndex);
    try (ArchiveIndexReader archiveIndexReader = new ArchiveIndexReader(archiveIndexPath)) {
      archiveIndexReader
          .getAllPackFiles()
          .forEach(
              packFile -> {
                log.info("packFile: {}", packFile);
              });
    }
  }

  @Test
  public void testFindFilenameInAllPackFilesOfIndex() throws IOException {

    int archiveIndex = 228715;

    // found offset 679190, hash 827905DC8B797D1E80EFF7CACB2A5606A30F90C91C2A74CDBAF306594E7C8813,
    // archive.229415.pack
    String targetFilename = "block_" + blockIdExt.toFilename();

    // found offset 1893399, hash C71E95AB52F673185AD62AC52F3DFEAFE2CB2DEA12B802061F770FDA498E2E97,
    // archive.234015.0:8000000000000000.pack
    //    String targetFilename = "block_" + blockIdExt.toFilename();

    String archiveIndexPath =
        ArchiveIndexReader.getArchiveIndexPath(TON_DB_ROOT_PATH, archiveIndex);
    try (ArchiveIndexReader archiveIndexReader = new ArchiveIndexReader(archiveIndexPath)) {
      archiveIndexReader
          .getAllPackFiles()
          .forEach(
              packFile -> {
                //                log.info("packFile: {}", packFile);
                // Get all hash->offset mappings
                Map<String, Long> allMappings = archiveIndexReader.getAllHashOffsetMappings();
                // For each hash, check what file it points to
                try (PackageReader packageReader = new PackageReader(packFile)) {
                  for (Map.Entry<String, Long> entry : allMappings.entrySet()) {
                    String hash = entry.getKey();
                    long offset = entry.getValue();

                    try {
                      PackageReader.PackageEntry packageEntry = packageReader.getEntryAt(offset);
                      String filename = packageEntry.getFilename();
                      //                      log.info("filename: {}", filename);

                      if (filename.equals(targetFilename)) {
                        log.info("*** FOUND THE CORRECT HASH! ***");
                        log.info("packFile: {}", packFile);
                        log.info("Hash: {}", hash);
                        log.info("Offset: {}", offset);
                        log.info("Filename: {}", filename);
                        log.info("block {}", packageEntry.getBlock());

                        log.info("*** ACTUAL WORKING OFFSET: {} ***", offset);
                        log.info("*** ENTRY DATA SIZE: {} ***", packageEntry.getData().length);

                        // Now we know what the correct hash should be!
                        return;
                      }
                    } catch (Exception e) {
                      // Skip invalid entries
                    }
                  }
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }
  }

  @Test
  public void findCorrectHashForBlock229441() throws IOException {

    // good 827905DC8B797D1E80EFF7CACB2A5606A30F90C91C2A74CDBAF306594E7C8813, offset 679190
    //    String targetFilename =
    //
    // "block_(-1,8000000000000000,229441):439233F8D4B99BAD7A2CC84FFE0D16150ADC0E1058BCDF82243D1445A75CA5BF:E24EA0E5F520135DA4FC0B0477E5440E0D1C4E7EDB2026941F0457376BB3D97E";
    // could not find in archive.229415.pack, but exist in archive.234015.0:8000000000000000.pack
    String targetFilename = "block_" + blockIdExt.toFilename();

    int packageId = 228715;
    String archiveIndexPath = ArchiveIndexReader.getArchiveIndexPath(TON_DB_ROOT_PATH, packageId);
    String packFilePath =
        "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db/archive/packages/arch0002/archive.234015.0:8000000000000000.pack";
    try (ArchiveIndexReader archiveIndexReader = new ArchiveIndexReader(archiveIndexPath)) {
      // Get all hash->offset mappings
      Map<String, Long> allMappings = archiveIndexReader.getAllHashOffsetMappings();

      // For each hash, check what file it points to
      try (PackageReader packageReader = new PackageReader(packFilePath)) {
        for (Map.Entry<String, Long> entry : allMappings.entrySet()) {
          String hash = entry.getKey();
          long offset = entry.getValue();

          try {
            PackageReader.PackageEntry packageEntry = packageReader.getEntryAt(offset);
            String filename = packageEntry.getFilename();
            log.info("filename: {}", filename);

            if (filename.equals(targetFilename)) {
              log.info("*** FOUND THE CORRECT HASH! ***");
              log.info("Hash: {}", hash);
              log.info("Offset: {}", offset);
              log.info("Filename: {}", filename);
              log.info("block {}", packageEntry.getBlock());

              log.info("*** ACTUAL WORKING OFFSET: {} ***", offset);
              log.info("*** ENTRY DATA SIZE: {} ***", packageEntry.getData().length);

              // Now we know what the correct hash should be!
              return;
            }
          } catch (Exception e) {
            // Skip invalid entries
          }
        }
      }

      log.error("Could not find hash for target filename: {}", targetFilename);
    }
  }

  @Test
  public void testArchiveIndexReaderGetExactPackFilenameAndParsePackgeEntries() throws IOException {
    int wc = 0;
    int seqno = 229441;
    int mcSeqno = 234048;
    long shard = 0x8000000000000000L;

    int packageId = 228715;

    String archiveIndexPath = ArchiveIndexReader.getArchiveIndexPath(TON_DB_ROOT_PATH, packageId);
    try (ArchiveIndexReader archiveIndexReader = new ArchiveIndexReader(archiveIndexPath)) {
      String packFilename =
          archiveIndexReader.getExactPackFilename(packageId, seqno, wc, shard, mcSeqno);
      log.info("packFilename: {}", packFilename);
      try (PackageReader packageReader = new PackageReader(packFilename)) {
        packageReader.forEachTyped(
            entry -> {
              String filename = entry.getFilename();
              log.info("filename: {}, value size {}", filename, entry.getData().length);
            });
      }
    }
  }

  @Test
  public void testArchiveIndexReader() throws IOException {

    int wc = -1;
    int seqno = 229441;

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {

      int packageId = reader.getArchiveIndexBySeqno(wc, seqno);
      log.info("packageId: {}", packageId);

      String archiveIndexPath = ArchiveIndexReader.getArchiveIndexPath(TON_DB_ROOT_PATH, packageId);

      try (ArchiveIndexReader archiveIndexReader = new ArchiveIndexReader(archiveIndexPath)) {
        byte[] sliced = archiveIndexReader.getIndexDb().get("status".getBytes());
        log.info("sliced: {}", new String(sliced));

        byte[] numOfSlices = archiveIndexReader.getIndexDb().get("slices".getBytes());
        log.info("numOfSlices: {}", new String(numOfSlices));

        byte[] sliceSize = archiveIndexReader.getIndexDb().get("slice_size".getBytes());
        log.info("sliceSize: {}", new String(sliceSize));

        for (int i = 0; i < Integer.parseInt(new String(numOfSlices)); i++) {
          byte[] pkgSize =
              archiveIndexReader.getIndexDb().get(("status." + i).getBytes()); // read package size

          byte[] pkgInfo =
              archiveIndexReader.getIndexDb().get(("info." + i).getBytes()); // read package info
          log.info("slicedPackageInfo {}, size {}", new String(pkgInfo), new String(pkgSize));
        }
      }
    }
  }

  @Test
  public void testGlobalIndexReadingIndex() throws IOException {

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {
      for (String path :
          reader.getArchivePackagesFromMainIndex()) { // list pack files without their slices
        log.info("path: {}", path);
      }
    }
  }

  @Test
  public void testGetAllArchivePackFilesByDirScan() throws IOException {

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {
      for (String path :
          reader.getAllArchivePackageByDirScan()) { // this is the fastest way to get all pack files
        log.info("path: {}", path);
      }
    }
  }

  @Test
  public void testGetAllArchivePackFilesByIndexScan() throws IOException {
    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {
      reader.getAllPackFiles().forEach(log::info);
    }
  }

  @Test
  public void testGetAllArchiveIndexes() throws IOException {
    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {
      reader
          .getAllArchiveIndexesIds()
          .forEach(
              id -> {
                String archiveIndexPath =
                    ArchiveIndexReader.getArchiveIndexPath(TON_DB_ROOT_PATH, id);
                log.info("{},  archiveIndexPath: {}", id, archiveIndexPath);
              });
    }
  }

  @Test
  public void testGlobalIndexReadingByPkgId() throws IOException {
    int packageId = 6400;
    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {
      Map<String, ArchiveFileLocation> result = reader.getPackageHashOffsetMappings(packageId);
      for (Map.Entry<String, ArchiveFileLocation> entry : result.entrySet()) {
        log.info("key {}, value: {}", entry.getKey(), entry.getValue());
        //        String packFilePath = entry.getValue().getIndexPath().replace("index", "pack");
        //        log.info("packFilePath: {}", packFilePath);
        //        packFilePath =
        //            packFilePath.replace("archive.06400.pack",
        // "archive.06400.0:8000000000000000.pack");
        //        PackageReader packageReader = new PackageReader(packFilePath);
        //        PackageReader.PackageEntry packageEntry =
        //            (PackageReader.PackageEntry)
        // packageReader.getEntryAt(entry.getValue().getOffset());
        //        log.info(
        //            "packageEntry: {}, dataSize {}, {}",
        //            packageEntry.getFilename(),
        //            packageEntry.getData().length,
        //            Utils.bytesToHex(packageEntry.getData()));
      }
    }
  }

  /** lists all proof_(-1,8000000000000000,125647):D35.... */
  @Test
  public void testGlobalIndexReadingIndexedPackages() throws IOException {

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {

      int count = 0;
      for (Map.Entry<String, ArchiveFileLocation> path :
          reader.getAllPackagesHashOffsetMappings().entrySet()) {

        PackageReader packageReader =
            new PackageReader(path.getValue().getIndexPath().replace("index", "pack"));
        PackageReader.PackageEntry packageEntry =
            packageReader.getEntryAt(path.getValue().getOffset());
        log.info(
            "packageEntry: {}, dataSize {}, {}",
            packageEntry.getFilename(),
            packageEntry.getData().length,
            Utils.bytesToHex(packageEntry.getData()));
        if (packageEntry.getFilename().startsWith("proof_")) { // "block_"
          log.info("Found block {}", packageEntry.getFilename());
          Block block = packageEntry.getBlock();
          log.info("block: {}", block);
        }

        packageReader.close();
        if (count++ > 5000) {
          break;
        }
      }
    }
  }

  /** Test finding ALL archive package files from filesystem (including missing ones). */
  @Test
  public void testAllArchivePackageFilesFromFilesystem() throws IOException {
    log.info("=== Testing All Archive Package Files From Filesystem ===");

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {

      // Test the original method (only finds files from global index)
      log.info("Files from global index method:");
      List<String> filesFromIndex = reader.getArchivePackagesFromMainIndex();
      for (String path : filesFromIndex) {
        log.info("  From index: {}", path);
      }
      log.info("Total files from global index: {}", filesFromIndex.size());

      log.info("");

      // Test the new method (finds ALL files from filesystem)
      log.info("Files from filesystem scan method:");
      List<String> filesFromFilesystem = reader.getAllArchivePackageByDirScan();
      for (String path : filesFromFilesystem) {
        log.info("  From filesystem: {}", path);
      }
      log.info("Total files from filesystem: {}", filesFromFilesystem.size());

      log.info("");

      // Show the difference
      log.info("=== COMPARISON ===");
      log.info("Files found by global index method: {}", filesFromIndex.size());
      log.info("Files found by filesystem scan method: {}", filesFromFilesystem.size());
      log.info("Missing files discovered: {}", filesFromFilesystem.size() - filesFromIndex.size());

      // Find files that are in filesystem but not in index
      List<String> missingFiles = new ArrayList<>(filesFromFilesystem);
      missingFiles.removeAll(filesFromIndex);

      if (!missingFiles.isEmpty()) {
        log.info("Missing files that were discovered:");
        for (String missingFile : missingFiles) {
          log.info("  MISSING: {}", missingFile);
        }
      }
    }
  }

  /** Test reading from individual archive index databases (C++ approach). */
  @Test
  public void testArchiveIndexDatabaseReading() throws IOException {
    log.info("=== Testing Archive Index Database Reading (C++ Approach) ===");

    try (GlobalIndexDbReader reader = new GlobalIndexDbReader(TON_DB_ROOT_PATH)) {

      // Method 1: Files from global index (original - limited)
      log.info("1. Files from Files database global index:");
      List<String> filesFromGlobalIndex = reader.getArchivePackagesFromMainIndex();
      log.info("   Found {} package files", filesFromGlobalIndex.size());

      log.info("");

      // Method 2: Files from filesystem scan (previous improvement)
      log.info("2. Files from filesystem scan:");
      List<String> filesFromFilesystem = reader.getAllArchivePackageByDirScan();
      log.info("   Found {} package files", filesFromFilesystem.size());

      log.info("");

      // Method 3: Files from archive index databases (NEW - C++ approach)
      log.info("3. Files from archive index databases (C++ approach):");
      Map<String, ArchiveFileLocation> fileLocations = reader.getAllPackagesHashOffsetMappings();
      log.info("   Found {} individual files with hash->offset mappings", fileLocations.size());

      // Show package file counts
      Map<Integer, Integer> packageFileCounts = reader.getArchivePackageFileCounts();
      log.info("   Package file counts:");
      for (Map.Entry<Integer, Integer> entry : packageFileCounts.entrySet()) {
        log.info("     Package {}: {} files", entry.getKey(), entry.getValue());
      }

      log.info("");

      // Show some sample file locations
      log.info("Sample file locations from archive indexes:");
      int count = 0;
      for (Map.Entry<String, ArchiveFileLocation> entry : fileLocations.entrySet()) {
        if (count++ < 5) {
          String hash = entry.getKey();
          ArchiveFileLocation location = entry.getValue();
          log.info(
              "  Hash: {} -> Package: {}, Offset: {}",
              hash.substring(0, 16) + "...",
              location.getPackageId(),
              location.getOffset());
        }
      }

      log.info("");

      // Final comparison
      log.info("=== FINAL COMPARISON ===");
      log.info("Method 1 (Global Index): {} package files", filesFromGlobalIndex.size());
      log.info("Method 2 (Filesystem Scan): {} package files", filesFromFilesystem.size());
      log.info(
          "Method 3 (Archive Index DBs): {} individual files in {} packages",
          fileLocations.size(),
          packageFileCounts.size());

      log.info("");
      log.info("The C++ approach (Method 3) provides complete access to individual files");
      log.info("within archive packages using hash->offset lookups, just like the original");
      log.info("TON implementation.");
    }
  }
}
