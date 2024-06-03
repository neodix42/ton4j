package org.ton.java.smartcontract;

import jdk.nashorn.internal.ir.annotations.Ignore;
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
import java.util.concurrent.*;

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

    @Ignore
    private String funcExecutable;
    @Ignore
    private String fiftExecutable;


    public static class FuncCompilerBuilder {
    }

    public static FuncCompilerBuilder builder() {
        return new CustomFuncCompilerBuilder();
    }

    private static class CustomFuncCompilerBuilder extends FuncCompilerBuilder {
        private String errorMsg = "Make sure you have fift and func installed. See https://github.com/ton-blockchain/packages for instructions.";
        private String funcAbsolutePath;
        private String fiftAbsolutePath;

        @Override
        public FuncCompiler build() {

            if (StringUtils.isEmpty(super.funcExecutablePath)) {
                System.out.println("checking if func is installed...");

                try {
                    ProcessBuilder pb = new ProcessBuilder("func", "-h").redirectErrorStream(true);
                    Process p = pb.start();
                    p.waitFor(5, TimeUnit.SECONDS);
                    if (p.exitValue() != 2) {
                        throw new Error("Cannot execute simple func command.\n" + errorMsg);
                    }
                    funcAbsolutePath = detectAbsolutePath("func");

                    System.out.println("func found at " + funcAbsolutePath);
                    super.funcExecutable = "func";

                } catch (Exception e) {
                    e.printStackTrace();
                    throw new Error("Cannot execute simple func command.\n" + errorMsg);
                }
            } else {
                System.out.println("using " + super.funcExecutablePath);
                super.funcExecutable = super.funcExecutablePath;
            }

            if (StringUtils.isEmpty(super.fiftExecutablePath)) {
                System.out.println("checking if fift is installed...");
                try {
                    ProcessBuilder pb = new ProcessBuilder("fift", "-h").redirectErrorStream(true);
                    Process p = pb.start();
                    p.waitFor(5, TimeUnit.SECONDS);
                    if (p.exitValue() != 2) {
                        throw new Error("Cannot execute simple fift command.\n" + errorMsg);
                    }
                    fiftAbsolutePath = detectAbsolutePath("fift");
                    System.out.println("fift found at " + fiftAbsolutePath);
                    super.fiftExecutable = "fift";
                } catch (Exception e) {
                    throw new Error("Cannot execute simple fift command.\n" + errorMsg);
                }
            } else {
                System.out.println("using " + super.fiftExecutablePath);
                super.fiftExecutable = super.fiftExecutablePath;
            }

            if (StringUtils.isEmpty(super.fiftAsmLibraryPath)) {
                super.fiftAsmLibraryPath = new File(fiftAbsolutePath).getParent() + File.separator + ".." + File.separator +
                        "lib" + File.separator + "ton" + File.separator + "bin" + File.separator + "lib";
            }

            if (StringUtils.isEmpty(super.fiftSmartcontLibraryPath)) {
                super.fiftSmartcontLibraryPath = new File(fiftAbsolutePath).getParent() + File.separator + ".." + File.separator +
                        "lib" + File.separator + "ton" + File.separator + "bin" + File.separator + "smartcont";
            }

            System.out.println("using FIFTPATH: " + super.fiftAsmLibraryPath + ":" + super.fiftSmartcontLibraryPath);

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

    private String executeFunc(String... params) throws ExecutionException, InterruptedException {
        Pair<Process, Future<String>> result = execute(funcExecutable, params);

        return result.getRight().get();
    }

    private String executeFift(String... params) {
        String[] withInclude = new String[]{"-I", fiftAsmLibraryPath + ":" + fiftSmartcontLibraryPath};
        String[] all = ArrayUtils.addAll(withInclude, params);
        Pair<Process, Future<String>> result = execute(fiftExecutable, all);
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

    private Pair<Process, Future<String>> execute(String pathToBinary, String... command) {

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

    private static String detectAbsolutePath(String executable) {
        try {
            ProcessBuilder pb;
            if (Utils.getOS() == Utils.OS.WINDOWS) {
                pb = new ProcessBuilder("where", executable).redirectErrorStream(true);
            } else {
                pb = new ProcessBuilder("which", executable).redirectErrorStream(true);
            }
            Process p = pb.start();
            p.waitFor(5, TimeUnit.SECONDS);
            return IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
        } catch (Exception e) {
            throw new Error("Cannot detect absolute path to executable " + executable);
        }
    }
}