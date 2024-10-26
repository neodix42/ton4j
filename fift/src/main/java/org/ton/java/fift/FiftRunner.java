package org.ton.java.fift;

import static java.util.Objects.nonNull;

import java.io.File;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.utils.Utils;

@Builder
@Slf4j
public class FiftRunner {
  String fiftAsmLibraryPath;
  String fiftSmartcontLibraryPath;
  String fiftExecutablePath;

  static String fiftExecutable = "";

  public static class FiftRunnerBuilder {}

  public static FiftRunnerBuilder builder() {
    return new CustomFiftRunnerBuilder();
  }

  private static class CustomFiftRunnerBuilder extends FiftRunnerBuilder {
    private String fiftAbsolutePath;

    @Override
    public FiftRunner build() {
      if (StringUtils.isEmpty(super.fiftExecutablePath)) {
        log.info("checking if fift is installed...");
        String errorMsg =
            "Make sure you have fift installed. See https://github.com/ton-blockchain/packages for instructions.\nYou can also specify full path via SmartContractCompiler.fiftExecutablePath().";
        try {
          ProcessBuilder pb = new ProcessBuilder("fift", "-h").redirectErrorStream(true);
          Process p = pb.start();
          p.waitFor(1, TimeUnit.SECONDS);
          if (p.exitValue() != 2) {
            throw new Error("Cannot execute simple fift command.\n" + errorMsg);
          }
          fiftAbsolutePath = Utils.detectAbsolutePath("fift", false);
          log.info("fift found at " + fiftAbsolutePath);
          fiftExecutable = "fift";
        } catch (Exception e) {
          throw new Error("Cannot execute simple fift command.\n" + errorMsg);
        }
      } else {
        log.info("using " + super.fiftExecutablePath);
        fiftExecutable = super.fiftExecutablePath;
      }

      if (StringUtils.isEmpty(super.fiftAsmLibraryPath)) {
        if (Utils.getOS() == Utils.OS.WINDOWS) {
          super.fiftAsmLibraryPath =
              new File(fiftAbsolutePath).getParent() + File.separator + "lib";
        } else if ((Utils.getOS() == Utils.OS.LINUX) || (Utils.getOS() == Utils.OS.LINUX_ARM)) {
          super.fiftAsmLibraryPath = "/usr/lib/fift";
        } else { // mac
          if (new File("/usr/local/lib/fift").exists()) {
            super.fiftAsmLibraryPath = "/usr/local/lib/fift";
          } else if (new File("/opt/homebrew/lib/fift").exists()) {
            super.fiftAsmLibraryPath = "/opt/homebrew/lib/fift";
          }
        }
      }

      if (StringUtils.isEmpty(super.fiftSmartcontLibraryPath)) {
        if (Utils.getOS() == Utils.OS.WINDOWS) {
          super.fiftSmartcontLibraryPath =
              new File(fiftAbsolutePath).getParent() + File.separator + "smartcont";
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
      log.info(
          "using include dirs: "
              + super.fiftAsmLibraryPath
              + ", "
              + super.fiftSmartcontLibraryPath);

      return super.build();
    }
  }

  public String run(String workdir, String... params) {
    String[] withInclude;
    if (Utils.getOS() == Utils.OS.WINDOWS) {
      withInclude =
          new String[] {"-I", "\"" + fiftAsmLibraryPath + "@" + fiftSmartcontLibraryPath + "\""};
    } else {
      withInclude = new String[] {"-I", fiftAsmLibraryPath + ":" + fiftSmartcontLibraryPath};
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

  public String runStdIn(String workdir, String stdin) {
    String withInclude;
    if (Utils.getOS() == Utils.OS.WINDOWS) {
      withInclude =
          "-I" + "\"" + fiftAsmLibraryPath + "@" + fiftSmartcontLibraryPath + "\"" + " -s -";
    } else {
      withInclude = "-I" + fiftAsmLibraryPath + ":" + fiftSmartcontLibraryPath + " -s -";
    }

    Pair<Process, String> result =
        Executor.executeStdIn(fiftExecutable, workdir, stdin, withInclude);
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
}
