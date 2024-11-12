package org.ton.java.fift;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.utils.Utils;

@Builder
@Slf4j
public class FiftRunner {
  String fiftAsmLibraryPath;
  String fiftSmartcontLibraryPath;
  public String fiftExecutablePath;
  private Boolean printInfo;

  public static String fiftExecutable = "";

  public static class FiftRunnerBuilder {}

  public static FiftRunnerBuilder builder() {
    return new CustomFiftRunnerBuilder();
  }

  private static class CustomFiftRunnerBuilder extends FiftRunnerBuilder {
    private String fiftAbsolutePath;

    @Override
    public FiftRunner build() {
      if (isNull(super.printInfo)) {
        super.printInfo = true;
      }

      if (StringUtils.isEmpty(super.fiftExecutablePath)) {
        if (super.printInfo) {
          log.info("Checking if Fift is installed...");
        }
        String errorMsg =
            "Make sure you have Fift installed. See https://github.com/ton-blockchain/packages for instructions.\nYou can also specify full path via SmartContractCompiler.fiftExecutablePath().";
        try {
          ProcessBuilder pb = new ProcessBuilder("fift", "-h").redirectErrorStream(true);
          Process p = pb.start();
          p.waitFor(1, TimeUnit.SECONDS);
          if (p.exitValue() != 2) {
            throw new Error("Cannot execute simple Fift command.\n" + errorMsg);
          }
          fiftAbsolutePath = Utils.detectAbsolutePath("fift", false);
          if (super.printInfo) {
            log.info("Fift found at " + fiftAbsolutePath);
          }
          fiftExecutable = "fift";
        } catch (Exception e) {
          throw new Error("Cannot execute simple Fift command.\n" + errorMsg);
        }
      } else {
        if (super.printInfo) {
          log.info("using " + super.fiftExecutablePath);
        }
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

      if (super.printInfo) {
        log.info(
            "using include dirs: "
                + super.fiftAsmLibraryPath
                + ", "
                + super.fiftSmartcontLibraryPath);
      }
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
    Pair<Process, String> result = execute(fiftExecutable, workdir, all);
    if (nonNull(result)) {
      try {
        return result.getRight();
      } catch (Exception e) {
        log.error("executeFift error " + e.getMessage());
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

    Pair<Process, String> result = executeStdIn(fiftExecutable, workdir, stdin, withInclude);
    if (nonNull(result)) {
      try {
        return result.getRight();
      } catch (Exception e) {
        log.error("executeFift error " + e.getMessage());
        return null;
      }
    } else {
      return null;
    }
  }

  public String getLibsPath() {
    if (Utils.getOS() == Utils.OS.WINDOWS) {
      return fiftAsmLibraryPath + "@" + fiftSmartcontLibraryPath;
    } else {
      return fiftAsmLibraryPath + ":" + fiftSmartcontLibraryPath;
    }
  }

  public String getFiftPath() {
    return Utils.detectAbsolutePath("fift", false);
  }

  public Pair<Process, String> execute(String pathToBinary, String workDir, String... command) {

    String[] withBinaryCommand = new String[] {pathToBinary};

    withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

    try {
      if (printInfo) {
        log.info("execute: {}", String.join(" ", withBinaryCommand));
      }

      final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);

      pb.directory(new File(workDir));
      Process p = pb.start();

      p.waitFor(1, TimeUnit.SECONDS);

      String resultInput = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());

      p.getInputStream().close();
      p.getErrorStream().close();
      p.getOutputStream().close();
      if (p.exitValue() == 2 || p.exitValue() == 0) {
        return Pair.of(p, resultInput);
      } else {
        log.error("exit value {}", p.exitValue());
        log.error(resultInput);
        throw new Exception("Error running " + Arrays.toString(withBinaryCommand));
      }

    } catch (final IOException e) {
      log.error(e.getMessage());
      return null;
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public Pair<Process, String> executeStdIn(
      String pathToBinary, String workDir, String stdin, String include) {

    try {
      final ProcessBuilder pb;
      String cmd = "";
      if (Utils.getOS() == Utils.OS.WINDOWS) {

        pb =
            new ProcessBuilder(
                    "powershell", "-c", "'" + stdin + "' | " + pathToBinary + " " + include)
                .redirectErrorStream(true);
        cmd =
            String.join(
                " ", "powershell", "-c", "'" + stdin + "' | " + pathToBinary + " " + include);
      } else { // linux & macos
        pb =
                new ProcessBuilder(
                        "/bin/bash", "-c",
                        "echo", "\"" + stdin + "\" | " + pathToBinary + " " + include )
                        .redirectErrorStream(true);
        cmd = String.join(" ", "/bin/bash", "-c",  "\"echo", "'" , stdin , "'|", pathToBinary + " " + include + "\"");
      }

      if (printInfo) {
        log.info("execute: {}", cmd);
      }

      pb.directory(new File(workDir));
      Process p = pb.start();

      p.waitFor(1, TimeUnit.SECONDS);

      String resultInput = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());

      p.getInputStream().close();
      p.getErrorStream().close();
      p.getOutputStream().close();
      if (p.exitValue() == 2 || p.exitValue() == 0) {
        return Pair.of(p, resultInput);
      } else {
        log.error("exit value {}", p.exitValue());
        log.error(resultInput);
        throw new Exception("Error running " + cmd);
      }

    } catch (final IOException e) {
      log.info(e.getMessage());
      return null;
    } catch (Exception e) {
      log.info(e.getMessage());
      throw new RuntimeException(e);
    }
  }
}
