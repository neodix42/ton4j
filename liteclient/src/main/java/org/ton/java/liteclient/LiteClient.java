package org.ton.java.liteclient;

import lombok.Builder;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.liteclient.api.ResultLastBlock;
import org.ton.java.liteclient.api.ResultListBlockTransactions;
import org.ton.java.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Log
@Builder
public class LiteClient {

    private static final String LITE_CLIENT_EXE = "lite-client.exe";
    private static final String LITE_CLIENT = "lite-client";

    private static LiteClient singleInstance = null;

    private String pathToLiteClientBinary;

    /**
     * if not specified and globalConfigAsString is null then integrated global-config.json is used;
     * <p>
     * if not specified and globalConfigAsString is filled then globalConfigAsString is used;
     * <p>
     * If not specified and testnet=true then integrated testnet-global.config.json is used;
     */
    private String pathToGlobalConfig;
    private int timeout;

    private String nodeName;

    /**
     * Ignored if pathToGlobalConfig is not null.
     */
    private boolean testnet;

//    private LiteClient() {
//
//    }

    public static class LiteClientBuilder {
    }

    public static LiteClientBuilder builder() {
        return new CustomLiteClientBuilder();
    }

    private static class CustomLiteClientBuilder extends LiteClientBuilder {
        @Override
        public LiteClient build() {

            try {
                String liteClientName;
                switch (Utils.getOS()) {
                    case LINUX:
                        liteClientName = "lite-client-linux-x86-64";
                        break;
                    case LINUX_ARM:
                        liteClientName = "lite-client-linux-arm64";
                        break;
                    case WINDOWS:
                    case WINDOWS_ARM:
                        liteClientName = "lite-client.exe";
                        break;
                    case MAC:
                        liteClientName = "lite-client-mac-x86-64";
                        break;
                    case MAC_ARM64:
                        liteClientName = "lite-client-mac-arm64";
                        break;
                    default:
                        throw new Error("Operating system is not supported!");
                }

                if (isNull(super.pathToLiteClientBinary)) {
                    super.pathToLiteClientBinary = liteClientName;
                }

                if (super.timeout == 0) {
                    super.timeout = 10;
                }

                if (isNull(super.pathToGlobalConfig)) {

                    InputStream config;
                    if (super.testnet) {
                        config = LiteClient.class.getClassLoader().getResourceAsStream("testnet-global.config.json");
                        File f = new File("testnet-global.config.json");
                        FileUtils.copyInputStreamToFile(config, f);
                        super.pathToGlobalConfig = f.getAbsolutePath();
                    } else {
                        config = LiteClient.class.getClassLoader().getResourceAsStream("global-config.json");
                        File f = new File("global.config.json");
                        FileUtils.copyInputStreamToFile(config, f);
                        super.pathToGlobalConfig = f.getAbsolutePath();
                    }

                    config.close();

                } else {
                    if (!Files.exists(Paths.get(super.pathToGlobalConfig))) {
                        throw new RuntimeException("Global config is not found in path: " + super.pathToGlobalConfig);
                    }
                }

                System.out.printf("Java Lite-Client configuration:\n" +
                                "Location: %s\n" +
                                "Path to global config: %s\n" +
                                "Testnet: %s%n",
                        super.pathToLiteClientBinary,
                        super.pathToGlobalConfig,
                        super.testnet);

            } catch (Exception e) {
                throw new RuntimeException("Error creating lite-client instance: " + e.getMessage());
            }
            return super.build();
        }
    }

    public String getLastCommand() {
        String command = "last";

        String binaryPath = pathToLiteClientBinary;

        String[] withBinaryCommand;
        withBinaryCommand = new String[]{binaryPath, "-t", "10", "-C", pathToGlobalConfig, "-c"};
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
            return LiteClientParser.parseRunMethodSeqno(executeRunMethod(contractAddress, "get_subwallet_id", ""));
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
     * @param resultLastBlock      - full block id
     * @param amountOfTransactions - if zero defaults to 100000
     * @return string result of lite-client output
     */
    public String executeListblocktrans(final ResultLastBlock resultLastBlock, final long amountOfTransactions) {
        final String command = String.format("listblocktrans %s %d", resultLastBlock.getFullBlockSeqno(),
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

    public String executeDumptrans(final ResultLastBlock resultLastBlock, final ResultListBlockTransactions tx) {
        final String command = String.format("dumptrans %s %d:%s %d", resultLastBlock.getFullBlockSeqno(), resultLastBlock.getWc(), tx.getAccountAddress(), tx.getLt());
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
        return LiteClientParser.parseRunMethodSeqno(executeRunMethod(electorAddr, "active_election_id", ""));
    }

    public String executeGetParticipantList(String electorAddr) throws Exception {
        // parseRunMethodParticipantList
        return executeRunMethod(electorAddr, "participant_list", "");
    }

    public String executeComputeReturnedStake(String electorAddr, String validatorWalletAddr) throws Exception {
        // parseRunMethodComputeReturnedStake
        //final String command = String.format("runmethod %s %s 0x%s", electorAddr, "compute_returned_stake", validatorWalletAddr);
        //log.info(command);
        return executeRunMethod(electorAddr, "compute_returned_stake", "0x" + validatorWalletAddr.trim().toLowerCase());
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
            List<ResultLastBlock> foundShardsInBlock = LiteClientParser.parseAllShards(executeAllshards(lastBlock));
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
        withBinaryCommand = new String[]{binaryPath, "-t", String.valueOf(timeout), "-C", pathToGlobalConfig, "-c"};

        //String[] withBinaryCommand = {binaryPath, "-C", forked ? node.getNodeForkedGlobalConfigLocation() : node.getNodeGlobalConfigLocation(), "-c"};
        withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

        try {
            log.info("execute: " + String.join(" ", withBinaryCommand));

            ExecutorService executorService = Executors.newSingleThreadExecutor();

            final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);

            pb.directory(new File(new File(binaryPath).getParent()));
            Process p = pb.start();

            Future<String> future = executorService.submit(() -> {
                try {
                    Thread.currentThread().setName("lite-client-" + nodeName);

                    String resultInput = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());

                    p.getInputStream().close();
                    p.getErrorStream().close();
                    p.getOutputStream().close();

                    return resultInput;

                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            });

            executorService.shutdown();

            return Pair.of(p, future);

        } catch (final IOException e) {
            log.info(e.getMessage());
            return null;
        }
    }
}
