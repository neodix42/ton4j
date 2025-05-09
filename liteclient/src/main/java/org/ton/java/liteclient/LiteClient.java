package org.ton.java.liteclient;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.liteclient.api.ResultLastBlock;
import org.ton.java.liteclient.api.ResultListBlockTransactions;
import org.ton.java.liteclient.api.block.Block;
import org.ton.java.liteclient.api.block.Transaction;

@Slf4j
@Builder
public class LiteClient {

  private static final String LITE_CLIENT_EXE = "lite-client.exe";
  private static final String LITE_CLIENT = "lite-client";

  private static LiteClient singleInstance = null;

  private String pathToLiteClientBinary;

  /** if not specified and globalConfigAsString is filled then globalConfigAsString is used; */
  private String pathToGlobalConfig;

  private int timeout;

  private String nodeName;

  /** Ignored if pathToGlobalConfig is not null. */
  private boolean testnet;

  private Boolean printInfo;

  public static class LiteClientBuilder {}

  public static LiteClientBuilder builder() {
    return new CustomLiteClientBuilder();
  }

  private static class CustomLiteClientBuilder extends LiteClientBuilder {
    @Override
    public LiteClient build() {

      try {

        if (isNull(super.printInfo)) {
          super.printInfo = true;
        }

        if (StringUtils.isEmpty(super.pathToLiteClientBinary)) {
          if (super.printInfo) {
            log.info("checking if lite-client is installed...");
          }
          String errorMsg =
              "You can specify full path via LiteClient.builder().pathToLiteClientBinary(Utils.getLiteClientGithubUrl()).\nMake sure you have lite-client installed. See https://github.com/ton-blockchain/packages for instructions.";
          try {
            ProcessBuilder pb = new ProcessBuilder("lite-client", "-h").redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor(1, TimeUnit.SECONDS);
            //            if (p.exitValue() != 2) {
            //              throw new Error("Cannot execute lite-client command.\n" + errorMsg);
            //            }
            super.pathToLiteClientBinary = detectAbsolutePath("lite-client", false);
            if (super.printInfo) {
              log.info("lite-client found at " + super.pathToLiteClientBinary);
            }

          } catch (Exception e) {
            throw new Error("Cannot execute simple lite-client command.\n" + errorMsg);
          }
        } else {
          super.pathToLiteClientBinary = getLocalOrDownload(super.pathToLiteClientBinary);
        }

        if (super.timeout == 0) {
          super.timeout = 10;
        }

        super.pathToGlobalConfig = resolvePathToGlobalConfig();

        if (super.printInfo) {
          log.info(
              String.format(
                  "Java Lite-Client configuration:\n"
                      + "Location: %s\n"
                      + "Path to global config: %s\n"
                      + "Testnet: %s%n",
                  super.pathToLiteClientBinary, super.pathToGlobalConfig, super.testnet));
        }
      } catch (Exception e) {
        throw new RuntimeException("Error creating lite-client instance: " + e.getMessage());
      }
      return super.build();
    }

    private String resolvePathToGlobalConfig() {
      String globalConfigPath;

      if (isNull(super.pathToGlobalConfig)) {
        if (super.testnet) {
          try {
            globalConfigPath = getLocalOrDownload(getGlobalConfigUrlTestnet());
          } catch (Error e) {
            globalConfigPath = getLocalOrDownload(getGlobalConfigUrlTestnetGithub());
          }
        } else {
          try {
            globalConfigPath = getLocalOrDownload(getGlobalConfigUrlMainnet());
          } catch (Error e) {
            globalConfigPath = getLocalOrDownload(getGlobalConfigUrlMainnetGithub());
          }
        }
      } else {
        globalConfigPath = getLocalOrDownload(super.pathToGlobalConfig);
      }

      if (!Files.exists(Paths.get(globalConfigPath))) {
        throw new RuntimeException(
            "Global config is not found in path: " + super.pathToGlobalConfig);
      }
      return globalConfigPath;
    }
  }

  /**
   * Detect the absolute path of an executable
   *
   * @param appName The application name
   * @param library Whether it's a library
   * @return The absolute path
   */
  public static String detectAbsolutePath(String appName, boolean library) {
    try {
      if (library) {
        appName = appName + "." + getLibraryExtension();
      }
      ProcessBuilder pb;
      OS os = getOS();
      if (os == OS.WINDOWS || os == OS.WINDOWS_ARM) {
        pb = new ProcessBuilder("where", appName).redirectErrorStream(true);
      } else {
        pb = new ProcessBuilder("which", appName).redirectErrorStream(true);
      }
      Process p = pb.start();
      p.waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
      String output =
          new BufferedReader(
                  new InputStreamReader(
                      p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))
              .lines()
              .collect(Collectors.joining("\n"));
      String[] paths = output.split("\n");
      if (paths.length == 1) {
        return paths[0];
      } else {
        for (String path : paths) {
          if (path.contains("ton")) {
            return org.apache.commons.lang3.StringUtils.trim(path);
          }
        }
      }
      return null;
    } catch (Exception e) {
      throw new Error(
          "Cannot detect absolute path to executable " + appName + ", " + e.getMessage());
    }
  }

  /** Enum for operating system types */
  public enum OS {
    WINDOWS,
    WINDOWS_ARM,
    LINUX,
    LINUX_ARM,
    MAC,
    MAC_ARM64,
    UNKNOWN
  }

  public static String getLibraryExtension() {
    OS os = getOS();
    if (os == OS.WINDOWS || os == OS.WINDOWS_ARM) {
      return "dll";
    } else if (os == OS.MAC || os == OS.MAC_ARM64) {
      return "dylib";
    } else {
      return "so";
    }
  }

  /**
   * Detect the operating system
   *
   * @return The detected operating system
   */
  public static OS getOS() {
    String os = System.getProperty("os.name").toLowerCase();
    String arch = System.getProperty("os.arch").toLowerCase();

    if (os.contains("win")) {
      return arch.contains("aarch64") ? OS.WINDOWS_ARM : OS.WINDOWS;
    } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
      return arch.contains("aarch64") || arch.contains("arm") ? OS.LINUX_ARM : OS.LINUX;
    } else if (os.contains("mac")) {
      return arch.contains("aarch64") || arch.contains("arm") ? OS.MAC_ARM64 : OS.MAC;
    }
    return OS.UNKNOWN;
  }

  /**
   * Get the URL for the mainnet global config
   *
   * @return The URL
   */
  public static String getGlobalConfigUrlMainnet() {
    return "https://ton.org/global-config.json";
  }

  /**
   * Get the URL for the testnet global config
   *
   * @return The URL
   */
  public static String getGlobalConfigUrlTestnet() {
    return "https://ton.org/testnet-global.config.json";
  }

  /**
   * Get the URL for the mainnet global config from GitHub
   *
   * @return The URL
   */
  public static String getGlobalConfigUrlMainnetGithub() {
    return "https://raw.githubusercontent.com/ton-blockchain/ton-blockchain.github.io/main/global.config.json";
  }

  /**
   * Get the URL for the testnet global config from GitHub
   *
   * @return The URL
   */
  public static String getGlobalConfigUrlTestnetGithub() {
    return "https://raw.githubusercontent.com/ton-blockchain/ton-blockchain.github.io/main/testnet-global.config.json";
  }

  /**
   * Download a file or return the local path
   *
   * @param linkToFile The link to the file
   * @return The local path
   */
  private static String getLocalOrDownload(String linkToFile) {
    if (linkToFile.contains("http") && linkToFile.contains("://")) {
      try {
        URL url = new URL(linkToFile);
        String filename = FilenameUtils.getName(url.getPath());
        File tmpFile = new File(filename);
        if (!tmpFile.exists()) {
          log.info("downloading {}", linkToFile);
          org.apache.commons.io.FileUtils.copyURLToFile(url, tmpFile);
          tmpFile.setExecutable(true);
        }
        return tmpFile.getAbsolutePath();
      } catch (Exception e) {
        log.error("Error downloading file", e);
        throw new Error("Cannot download file. Error " + e.getMessage());
      }
    } else {
      return linkToFile;
    }
  }

  public String getLastCommand() {
    String command = "last";

    String binaryPath = pathToLiteClientBinary;

    String[] withBinaryCommand;
    withBinaryCommand = new String[] {binaryPath, "-t", "10", "-C", pathToGlobalConfig, "-c"};
    withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

    return String.join(" ", withBinaryCommand);
  }

  public String executeLast() {
    String command = "last";
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      try {
        return result.getRight().get();
      } catch (Exception e) {
        log.info("executeLast error " + e.getMessage());
        return null;
      }
    } else {
      return null;
    }
  }

  public long executeGetSeqno(String contractAddress) {
    try {
      return LiteClientParser.parseRunMethodSeqno(executeRunMethod(contractAddress, "seqno", ""));
    } catch (Exception e) {
      return -1L;
    }
  }

  public long executeGetSubWalletId(String contractAddress) {
    try {
      return LiteClientParser.parseRunMethodSeqno(
          executeRunMethod(contractAddress, "get_subwallet_id", ""));
    } catch (Exception e) {
      return -1L;
    }
  }

  /**
   * @param seqno - is the pureBlockSeqno
   * @return string result of lite-client output
   */
  public String executeBySeqno(long wc, String shard, BigInteger seqno) throws Exception {
    final String command = String.format("byseqno %d:%s %d", wc, shard, seqno);
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      return result.getRight().get();
    } else {
      return null;
    }
  }

  /**
   * @param resultLastBlock - full block id
   * @param amountOfTransactions - if zero defaults to 100000
   * @return string result of lite-client output
   */
  public String executeListblocktrans(
      final ResultLastBlock resultLastBlock, final long amountOfTransactions) {
    final String command =
        String.format(
            "listblocktrans %s %d",
            resultLastBlock.getFullBlockSeqno(),
            (amountOfTransactions == 0) ? 100000 : amountOfTransactions);
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      try {
        return result.getRight().get();
      } catch (Exception e) {
        log.info("executeListblocktrans error " + e.getMessage());
        return null;
      }
    } else {
      return null;
    }
  }

  public String executeDumptrans(
      final ResultLastBlock resultLastBlock, final ResultListBlockTransactions tx) {
    final String command =
        String.format(
            "dumptrans %s %d:%s %d",
            resultLastBlock.getFullBlockSeqno(),
            resultLastBlock.getWc(),
            tx.getAccountAddress(),
            tx.getLt());
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      try {
        return result.getRight().get();
      } catch (Exception e) {
        log.info("executeDumptrans error " + e.getMessage());
        return null;
      }
    } else {
      return null;
    }
  }

  public String executeDumptrans(String tx) {
    final String command = String.format("dumptrans %s", tx);
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      try {
        return result.getRight().get();
      } catch (Exception e) {
        log.info("executeDumptrans error " + e.getMessage());
        return null;
      }
    } else {
      return null;
    }
  }

  public String executeDumpblock(final ResultLastBlock resultLastBlock) {
    final String command = String.format("dumpblock %s", resultLastBlock.getFullBlockSeqno());
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      try {
        return result.getRight().get();
      } catch (Exception e) {
        log.info("executeDumpblock error " + e.getMessage());
        return null;
      }
    } else {
      return null;
    }
  }

  public String executeDumpblock(String fullBlockSeqno) {
    final String command = String.format("dumpblock %s", fullBlockSeqno);
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      try {
        return result.getRight().get();
      } catch (Exception e) {
        log.info("executeDumpblock error " + e.getMessage());
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * executes 2 calls against lite-server
   *
   * <pre>
   * 1. last
   * 2. dumpblock
   * </pre>
   *
   * @return List<Transaction>
   */
  public List<Transaction> getAllTransactionsFromLatestBlock() {
    try {
      Block lastBlock =
          LiteClientParser.parseDumpblock(
              executeDumpblock(LiteClientParser.parseLast(executeLast())), false, false);
      return lastBlock.listBlockTrans();
    } catch (Exception e) {
      throw new Error("Cannot retrieve all transactions from last block");
    }
  }

  public List<Transaction> getAllTransactionsByBlock(ResultLastBlock block) {
    try {
      Block lastBlock = LiteClientParser.parseDumpblock(executeDumpblock(block), false, true);
      return lastBlock.listBlockTrans();
    } catch (Exception e) {
      throw new Error("Cannot retrieve all transactions from block");
    }
  }

  public List<Transaction> getAccountTransactionsByBlock(ResultLastBlock block, String address) {
    try {
      Block lastBlock = LiteClientParser.parseDumpblock(executeDumpblock(block), false, true);
      return lastBlock.listBlockTrans(address);
    } catch (Exception e) {
      throw new Error("Cannot retrieve all transactions from block");
    }
  }

  /**
   * executes 3+x calls against lite-server
   *
   * <pre>
   * 1. last
   * 2. dumpblock latest master chain
   * 3. allshards
   * 4. dumpblock latest block for each shard
   * </pre>
   *
   * @return List<Transaction>
   */
  public List<Transaction> getAllTransactionsFromLatestBlockAndAllShards() {
    try {
      List<Transaction> txs;
      ResultLastBlock blockIdLast = LiteClientParser.parseLast(executeLast());

      txs = getAllTransactionsByBlock(blockIdLast);

      List<ResultLastBlock> shards = LiteClientParser.parseAllShards(executeAllshards(blockIdLast));
      for (ResultLastBlock shard : shards) {
        List<Transaction> shardTxs = getAllTransactionsByBlock(shard);
        txs.addAll(shardTxs);
      }
      return txs;
    } catch (Exception e) {
      e.printStackTrace();
      throw new Error("Cannot retrieve all transactions from all shards");
    }
  }

  /**
   * executes 3+x calls against lite-server
   *
   * <pre>
   * 1. last
   * 2. dumpblock latest master chain
   * 3. allshards
   * 4. dumpblock latest block for each shard
   * </pre>
   *
   * @return List<Transaction>
   */
  public List<Transaction> getAccountTransactionsFromLatestBlockAndAllShards(String address) {
    try {
      List<Transaction> txs;
      ResultLastBlock blockIdLast = LiteClientParser.parseLast(executeLast());

      txs = getAccountTransactionsByBlock(blockIdLast, address);

      List<ResultLastBlock> shards = LiteClientParser.parseAllShards(executeAllshards(blockIdLast));
      for (ResultLastBlock shard : shards) {
        List<Transaction> shardTxs = getAccountTransactionsByBlock(shard, address);
        txs.addAll(shardTxs);
      }
      return txs;
    } catch (Exception e) {
      e.printStackTrace();
      throw new Error("Cannot retrieve all transactions from all shards");
    }
  }

  /**
   * executes 3+x calls against lite-server
   *
   * <pre>
   * 1. last
   * 2. dumpblock latest master chain
   * 3. allshards
   * 4. dumpblock latest block for each shard
   * </pre>
   *
   * @return List<Transaction>
   */
  public List<Transaction> getAccountTransactionsFromBlockAndAllShards(
      ResultLastBlock blockId, String address) {
    try {
      List<Transaction> txs;

      txs = getAccountTransactionsByBlock(blockId, address);

      List<ResultLastBlock> shards = LiteClientParser.parseAllShards(executeAllshards(blockId));
      for (ResultLastBlock shard : shards) {
        List<Transaction> shardTxs = getAccountTransactionsByBlock(shard, address);
        txs.addAll(shardTxs);
      }
      return txs;
    } catch (Exception e) {
      e.printStackTrace();
      throw new Error("Cannot retrieve all transactions from all shards");
    }
  }

  public String executeAllshards(final ResultLastBlock resultLastBlock) throws Exception {
    final String command = "allshards " + resultLastBlock.getFullBlockSeqno();
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      return result.getRight().get();
    } else {
      return null;
    }
  }

  public String executeGetAccount(String address) {
    final String command = "getaccount " + address;
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      try {
        return result.getRight().get();
      } catch (Exception e) {
        log.info("executeGetAccount error " + e.getMessage());
        return null;
      }
    } else {
      return null;
    }
  }

  public String executeRunMethod(String address, String methodId, String params) throws Exception {
    final String command = String.format("runmethod %s %s %s", address, methodId, params);
    return execute(command).getRight().get();
  }

  /**
   * @param address base64 or raw
   * @param blockId in format (-1,8000000000000000,20301335):root-hash:file-hash
   * @param methodId method name, e.g. seqno
   * @param params space separated params
   * @return lite-client output
   */
  public String executeRunMethod(String address, String blockId, String methodId, String params)
      throws Exception {
    final String command =
        String.format("runmethod %s %s %s %s", address, blockId, methodId, params);
    log.info(command);
    return execute(command).getRight().get();
  }

  /**
   * @param address base64 or raw
   * @param blockId ResultLastBlock
   * @param methodId method name, e.g. seqno
   * @param params space separated params
   * @return lite-client output
   */
  public String executeRunMethod(
      String address, ResultLastBlock blockId, String methodId, String params) throws Exception {
    final String command =
        String.format(
            "runmethod %s %s %s %s", address, blockId.getFullBlockSeqno(), methodId, params);
    log.info(command);
    return execute(command).getRight().get();
  }

  public String executeSendfile(String absolutePathFile) throws Exception {
    final String command = "sendfile " + absolutePathFile;
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      return result.getRight().get();
    } else {
      return null;
    }
  }

  public String executeGetElections() throws Exception {
    //
    final String command = "getconfig 15";
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      return result.getRight().get();
    } else {
      return null;
    }
  }

  public String executeGetConfigSmcAddress() throws Exception {
    final String command = "getconfig 0";
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      return result.getRight().get();
    } else {
      return null;
    }
  }

  public String executeGetElectorSmcAddress() throws Exception {
    final String command = "getconfig 1";
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      return result.getRight().get();
    } else {
      return null;
    }
  }

  public String executeGetMinterSmcAddress() throws Exception {
    final String command = "getconfig 2";
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      return result.getRight().get();
    } else {
      return null;
    }
  }

  // start of the validation cycle
  public long executeGetActiveElectionId(String electorAddr) throws Exception {
    return LiteClientParser.parseRunMethodSeqno(
        executeRunMethod(electorAddr, "active_election_id", ""));
  }

  public String executeGetParticipantList(String electorAddr) throws Exception {
    // parseRunMethodParticipantList
    return executeRunMethod(electorAddr, "participant_list", "");
  }

  public String executeComputeReturnedStake(String electorAddr, String validatorWalletAddr)
      throws Exception {
    // parseRunMethodComputeReturnedStake
    // final String command = String.format("runmethod %s %s 0x%s", electorAddr,
    // "compute_returned_stake", validatorWalletAddr);
    // log.info(command);
    return executeRunMethod(
        electorAddr, "compute_returned_stake", "0x" + validatorWalletAddr.trim().toLowerCase());
  }

  public String executeGetMinMaxStake() throws Exception {
    final String command = "getconfig 17";
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      return result.getRight().get();
    } else {
      return null;
    }
  }

  public String executeGetPreviousValidators() throws Exception {
    final String command = "getconfig 32";
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      return result.getRight().get();
    } else {
      return null;
    }
  }

  public String executeGetCurrentValidators() throws Exception {
    final String command = "getconfig 34";
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      return result.getRight().get();
    } else {
      return null;
    }
  }

  public String executeGetNextValidators() throws Exception {
    final String command = "getconfig 36";
    Pair<Process, Future<String>> result = execute(command);
    if (nonNull(result)) {
      return result.getRight().get();
    } else {
      return null;
    }
  }

  public List<ResultLastBlock> getShardsFromBlock(ResultLastBlock lastBlock) {
    try {
      List<ResultLastBlock> foundShardsInBlock =
          LiteClientParser.parseAllShards(executeAllshards(lastBlock));
      log.info("found " + foundShardsInBlock.size() + " shards in block " + foundShardsInBlock);
      return foundShardsInBlock;
    } catch (Exception e) {
      log.info("Error retrieving shards from the block " + e.getMessage());
      return null;
    }
  }

  public Pair<Process, Future<String>> execute(String... command) {

    String binaryPath = pathToLiteClientBinary;
    String[] withBinaryCommand;
    withBinaryCommand =
        new String[] {binaryPath, "-t", String.valueOf(timeout), "-C", pathToGlobalConfig, "-c"};

    withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

    try {
      //      log.debug("execute: " + String.join(" ", withBinaryCommand));

      ExecutorService executorService = Executors.newSingleThreadExecutor();

      final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);

      pb.directory(new File(new File(binaryPath).getParent()));
      Process p = pb.start();
      p.waitFor(1, TimeUnit.SECONDS);
      Future<String> future =
          executorService.submit(
              () -> {
                try {
                  Thread.currentThread().setName("lite-client-" + nodeName);

                  String resultInput =
                      IOUtils.toString(p.getInputStream(), Charset.defaultCharset());

                  p.getInputStream().close();
                  p.getErrorStream().close();
                  p.getOutputStream().close();

                  return resultInput;

                } catch (IOException e) {
                  log.info(e.getMessage());
                  return null;
                }
              });

      executorService.shutdown();

      return Pair.of(p, future);

    } catch (final IOException | InterruptedException e) {
      log.info(e.getMessage());
      return null;
    }
  }

  public String getLiteClientPath() {
    return detectAbsolutePath("lite-client", false);
  }
}
