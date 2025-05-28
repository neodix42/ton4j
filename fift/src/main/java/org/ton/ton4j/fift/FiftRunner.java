package org.ton.ton4j.fift;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.ton4j.utils.Utils;

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
            "You can specify full path via FiftRunner.builder().fiftExecutablePath(Utils.getFiftGithubUrl()).\nOr make sure you have Fift installed. See https://github.com/ton-blockchain/packages for instructions.";
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
        if (super.fiftExecutablePath.contains("http") && super.fiftExecutablePath.contains("://")) {
          try {
            String smartcont =
                StringUtils.substringBeforeLast(super.fiftExecutablePath, "/")
                    + "/smartcont_lib.zip";

            File tmpFileSmartcont =
                new File(System.getProperty("user.dir") + "/smartcont/stdlib.fc");
            if (!tmpFileSmartcont.exists()) {
              String smartcontPath = Utils.getLocalOrDownload(smartcont);
              ZipFile zipFile = new ZipFile(smartcontPath);
              zipFile.extractAll(new File(smartcontPath).getParent());
              Files.delete(Paths.get(smartcontPath));
            } else {
              //              log.info("smartcont_lib.zip already downloaded");
            }
          } catch (Exception e) {
            log.error("cannot download smartcont_lib.zip");
          }
        }
        super.fiftExecutablePath = Utils.getLocalOrDownload(super.fiftExecutablePath);
        if (StringUtils.isEmpty(super.fiftAsmLibraryPath)) {
          super.fiftAsmLibraryPath = new File(super.fiftExecutablePath).getParent() + "/lib";
        }
        if (StringUtils.isEmpty(super.fiftSmartcontLibraryPath)) {
          super.fiftSmartcontLibraryPath =
              new File(super.fiftExecutablePath).getParent() + "/smartcont";
        }
        if (super.printInfo) {
          log.info("Using {}", super.fiftExecutablePath);
        }
        fiftExecutable = super.fiftExecutablePath;
      }

      if (isNull(fiftAbsolutePath)) {
        fiftAbsolutePath = super.fiftExecutablePath;
      }

      if (StringUtils.isEmpty(super.fiftAsmLibraryPath)) {
        super.fiftAsmLibraryPath = getFiftSystemLibPath();
      }

      if (StringUtils.isEmpty(super.fiftSmartcontLibraryPath)) {
        super.fiftSmartcontLibraryPath = getFiftSystemSmartcontPath();
      }

      if (super.printInfo) {
        log.info(
            "Using include dirs: {}, {}", super.fiftAsmLibraryPath, super.fiftSmartcontLibraryPath);
      }
      return super.build();
    }

    private String getFiftSystemLibPath() {
      if (Utils.getOS() == Utils.OS.WINDOWS) {
        return new File(fiftAbsolutePath).getParent() + File.separator + "lib";
      } else if ((Utils.getOS() == Utils.OS.LINUX) || (Utils.getOS() == Utils.OS.LINUX_ARM)) {
        return "/usr/lib/fift";
      } else { // mac
        if (new File("/usr/local/lib/fift").exists()) {
          return "/usr/local/lib/fift";
        } else if (new File("/opt/homebrew/lib/fift").exists()) {
          return "/opt/homebrew/lib/fift";
        }
      }
      return null;
    }

    private String getFiftSystemSmartcontPath() {
      if (Utils.getOS() == Utils.OS.WINDOWS) {
        return new File(fiftAbsolutePath).getParent() + File.separator + "smartcont";
      }
      if ((Utils.getOS() == Utils.OS.LINUX) || (Utils.getOS() == Utils.OS.LINUX_ARM)) {
        return "/usr/share/ton/smartcont";
      } else { // mac
        if (new File("/usr/local/share/ton/ton/smartcont").exists()) {
          return "/usr/local/share/ton/ton/smartcont";
        } else if (new File("/opt/homebrew/share/ton/ton/smartcont").exists()) {
          return "/opt/homebrew/share/ton/ton/smartcont";
        }
      }
      return null;
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
    if (StringUtils.isNotEmpty(fiftExecutablePath)) {
      return fiftExecutablePath;
    }
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
                    "/bin/bash",
                    "-c",
                    "echo",
                    "\"" + stdin + "\" | " + pathToBinary + " " + include)
                .redirectErrorStream(true);
        cmd =
            String.join(
                " ",
                "/bin/bash",
                "-c",
                "\"echo",
                "'",
                stdin,
                "'|",
                pathToBinary + " " + include + "\"");
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
