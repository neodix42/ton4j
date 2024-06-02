package org.ton.java.smartcontract;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.Objects.nonNull;

/**
 * Make sure you have fift and func installed. See <a href="https://github.com/ton-blockchain/packages">packages</a> for instructions.
 */
@Builder
@Getter
@Log
public class FuncCompiler {

    String contractPath;
    String funcExecutablePath;
    String fiftExecutablePath;
    String fiftAsmLibraryPath;
    String fiftSmartcontLibraryPath;

    public static class FuncCompilerBuilder {
    }

    public static FuncCompilerBuilder builder() {
        return new CustomFuncCompilerBuilder();
    }

    private static class CustomFuncCompilerBuilder extends FuncCompilerBuilder {
        @Override
        public FuncCompiler build() {

            return super.build();
        }
    }

    /**
     * @return code of BoC in hex
     */
    public String compile() throws IOException, ExecutionException, InterruptedException {

        String outputFiftAsmFile = executeFunc("-W", "dummy.boc", contractPath);
        outputFiftAsmFile = StringUtils.replace(outputFiftAsmFile, "2 boc+>B", "0 boc+>B");
        File file = new File(new File(contractPath).getParent() + "/dummy.fif");

        FileUtils.writeStringToFile(file, outputFiftAsmFile, Charset.defaultCharset());

        // output binary boc file
        executeFift("-s", file.getAbsolutePath());
        byte[] bocContent = FileUtils.readFileToByteArray(new File(file.getParent() + "/dummy.boc"));
        return Utils.bytesToHex(bocContent);
    }

    public String executeFunc(String... params) throws ExecutionException, InterruptedException {
        Pair<Process, Future<String>> result = execute("func", params);

        return result.getRight().get();
    }

    public String executeFift(String... params) {
        String[] withInclude = new String[]{"-I", fiftAsmLibraryPath + ":" + fiftSmartcontLibraryPath};
        String[] all = ArrayUtils.addAll(withInclude, params);
        Pair<Process, Future<String>> result = execute("fift", all);
        if (nonNull(result)) {
            try {
                return result.getRight().get();
            } catch (Exception e) {
                log.info("executeFift error " + e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public Pair<Process, Future<String>> execute(String pathToBinary, String... command) {

        String[] withBinaryCommand = new String[]{pathToBinary};

        withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

        try {
            log.info("execute: " + String.join(" ", withBinaryCommand));

            ExecutorService executorService = Executors.newSingleThreadExecutor();

            final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);

            pb.directory(new File(new File(contractPath).getParent()));
            Process p = pb.start();

            Future<String> future = executorService.submit(() -> {

                Thread.currentThread().setName(pathToBinary);

                String resultInput = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());

                p.getInputStream().close();
                p.getErrorStream().close();
                p.getOutputStream().close();
                if (p.exitValue() != 0) {
                    log.info("exit value " + p.exitValue());
                    log.info(resultInput);
                    throw new Exception("Cannot compile smart-contract.");

                }
                return resultInput;
            });

            executorService.shutdown();

            return Pair.of(p, future);

        } catch (final IOException e) {
            log.info(e.getMessage());
            return null;
        }
    }
}