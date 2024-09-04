package org.ton.java.smartcontract;

import jdk.nashorn.internal.ir.annotations.Ignore;
import lombok.Builder;
import lombok.extern.java.Log;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.utils.Utils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.nonNull;

@Builder
@Log
public class FiftRunner {
    String fiftAsmLibraryPath;
    String fiftSmartcontLibraryPath;
    String fiftExecutablePath;

    @Ignore
    private String fiftExecutable;

    public static class FiftRunnerBuilder {
    }

    public static FiftRunnerBuilder builder() {
        return new CustomFiftRunnerBuilder();
    }

    private static class CustomFiftRunnerBuilder extends FiftRunnerBuilder {
        private String fiftAbsolutePath;

        @Override
        public FiftRunner build() {
            if (StringUtils.isEmpty(super.fiftExecutablePath)) {
                log.info("checking if fift is installed...");
                String errorMsg = "Make sure you have fift installed. See https://github.com/ton-blockchain/packages for instructions.\nYou can also specify full path via SmartContractCompiler.fiftExecutablePath().";
                try {
                    ProcessBuilder pb = new ProcessBuilder("fift", "-h").redirectErrorStream(true);
                    Process p = pb.start();
                    p.waitFor(1, TimeUnit.SECONDS);
                    if (p.exitValue() != 2) {
                        throw new Error("Cannot execute simple fift command.\n" + errorMsg);
                    }
                    fiftAbsolutePath = detectAbsolutePath();
                    log.info("fift found at " + fiftAbsolutePath);
                    super.fiftExecutable = "fift";
                } catch (Exception e) {
                    throw new Error("Cannot execute simple fift command.\n" + errorMsg);
                }
            } else {
                log.info("using " + super.fiftExecutablePath);
                super.fiftExecutable = super.fiftExecutablePath;
            }

            if (StringUtils.isEmpty(super.fiftAsmLibraryPath)) {
                if (Utils.getOS() == Utils.OS.WINDOWS) {
                    super.fiftAsmLibraryPath = new File(fiftAbsolutePath).getParent() + File.separator + "lib";
                } else if ((Utils.getOS() == Utils.OS.LINUX) || (Utils.getOS() == Utils.OS.LINUX_ARM)) {
                    super.fiftAsmLibraryPath = "/usr/lib/fift";
                } else { //mac
                    if (new File("/usr/local/lib/fift").exists()) {
                        super.fiftAsmLibraryPath = "/usr/local/lib/fift";
                    } else if (new File("/opt/homebrew/lib/fift").exists()) {
                        super.fiftAsmLibraryPath = "/opt/homebrew/lib/fift";
                    }
                }
            }

            if (StringUtils.isEmpty(super.fiftSmartcontLibraryPath)) {
                if (Utils.getOS() == Utils.OS.WINDOWS) {
                    super.fiftSmartcontLibraryPath = new File(fiftAbsolutePath).getParent() + File.separator + "smartcont";
                }
                if ((Utils.getOS() == Utils.OS.LINUX) || (Utils.getOS() == Utils.OS.LINUX_ARM)) {
                    super.fiftSmartcontLibraryPath = "/usr/share/ton/smartcont";
                } else { // mac
                    if (new File("/usr/local/share/ton/ton/smartcont").exists()) {
                        super.fiftSmartcontLibraryPath = "/usr/local/share/ton/ton/smartcont";
                    } else if (new File("/opt/homebrew/share/ton/ton/smartcont").exists()) {
                        super.fiftSmartcontLibraryPath = "/opt/homebrew/share/ton/ton/smartcont";
                    }
                }
            }
            log.info("using include dirs: " + super.fiftAsmLibraryPath + ", " + super.fiftSmartcontLibraryPath);

            return super.build();
        }
    }

    public String run(String workdir, String... params) {
        String[] withInclude;
        if (Utils.getOS() == Utils.OS.WINDOWS) {
            withInclude = new String[]{"-I", "\"" + fiftAsmLibraryPath + "@" + fiftSmartcontLibraryPath + "\""};
        } else {
            withInclude = new String[]{"-I", fiftAsmLibraryPath + ":" + fiftSmartcontLibraryPath};
        }
        String[] all = ArrayUtils.addAll(withInclude, params);
        Pair<Process, String> result = Executor.execute(fiftExecutable, workdir, all);
        if (nonNull(result)) {
            try {
                return result.getRight();
            } catch (Exception e) {
                log.info("executeFift error " + e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    private static String detectAbsolutePath() {
        try {
            ProcessBuilder pb;
            if (Utils.getOS() == Utils.OS.WINDOWS) {
                pb = new ProcessBuilder("where", "fift").redirectErrorStream(true);
            } else {
                pb = new ProcessBuilder("which", "fift").redirectErrorStream(true);
            }
            Process p = pb.start();
            p.waitFor(1, TimeUnit.SECONDS);
            String output = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
            String[] paths = output.split("\n");
            if (paths.length == 1) {
                return paths[0];
            } else {
                for (String path : paths) {
                    if (path.contains("ton")) {
                        return StringUtils.trim(path);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new Error("Cannot detect absolute path to executable fift " + e.getMessage());
        }
    }
}
