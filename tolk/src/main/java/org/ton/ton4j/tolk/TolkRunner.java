package org.ton.ton4j.tolk;

import static java.util.Objects.isNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
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
public class TolkRunner {

  public String tolkExecutablePath;
  public String tolkStdLibPath;
  private Boolean printInfo;

  public static String tolkExecutable = "";

  public static class TolkRunnerBuilder {}

  public static TolkRunnerBuilder builder() {
    return new CustomTolkRunnerBuilder();
  }

  private static class CustomTolkRunnerBuilder extends TolkRunnerBuilder {

    @Override
    public TolkRunner build() {

      if (isNull(super.printInfo)) {
        super.printInfo = true;
      }

      if (StringUtils.isEmpty(super.tolkExecutablePath)) {
        if (super.printInfo) {
          log.info("Checking if Tolk is installed...");
        }

        String errorMsg =
            "You can specify full path via TolkRunner.builder().tolkExecutablePath(Utils.getTolkGithubUrl()).\nOr make sure you have Tolk installed. See https://github.com/ton-blockchain/packages for instructions.";
        try {
          ProcessBuilder pb = new ProcessBuilder("tolk", "-h").redirectErrorStream(true);
          Process p = pb.start();
          p.waitFor(1, TimeUnit.SECONDS);
          if (p.exitValue() != 2) {
            throw new Error("Cannot execute simple Tolk command.\n" + errorMsg);
          }
          String tolkAbsolutePath = Utils.detectAbsolutePath("tolk", false);

          if (StringUtils.isEmpty(super.tolkStdLibPath)) {
            super.tolkStdLibPath = getTolkSystemStdLibPath();
          }
          if (super.printInfo) {
            log.info("Tolk found at {}, TOLK_STDLIB={} ", tolkAbsolutePath, super.tolkStdLibPath);
          }
          tolkExecutable = "tolk";

        } catch (Exception e) {
          log.info(e.getMessage());
          throw new Error("Cannot execute simple Tolk command.\n" + errorMsg);
        }
      } else {
        if (super.tolkExecutablePath.contains("http") && super.tolkExecutablePath.contains("://")) {
          try {
            String smartcont =
                StringUtils.substringBeforeLast(super.tolkExecutablePath, "/")
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

        super.tolkExecutablePath = Utils.getLocalOrDownload(super.tolkExecutablePath);
        if (StringUtils.isEmpty(super.tolkStdLibPath)) {
          super.tolkStdLibPath =
              new File(super.tolkExecutablePath).getParent() + "/smartcont/tolk-stdlib";
        }

        if (super.printInfo) {
          log.info("Using {}, TOLK_STDLIB={}", super.tolkExecutablePath, super.tolkStdLibPath);
        }
        tolkExecutable = super.tolkExecutablePath;
      }

      return super.build();
    }

    private String getTolkSystemStdLibPath() {
      if (Utils.getOS() == Utils.OS.WINDOWS) {
        return "C:/ProgramData/chocolatey/lib/ton/bin/smartcont/tolk-stdlib";
      } else if (Utils.getOS() == Utils.OS.MAC) {
        return "/opt/homebrew/share/ton/ton/smartcont/tolk-stdlib";
      } else {
        return "/usr/share/ton/smartcont/tolk-stdlib";
      }
    }
  }

  public String run(String workdir, String... params) {
    Pair<Process, String> result = execute(tolkExecutable, workdir, params);

    if (result != null && result.getRight() != null) {
      return result.getRight();
    }

    return "";
  }

  public String getTolkSystemPath() {
    return Utils.detectAbsolutePath("tolk", false);
  }

  public Pair<Process, String> execute(String pathToBinary, String workDir, String... command) {

    String[] withBinaryCommand = new String[] {pathToBinary};

    withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

    try {
      if (printInfo) {
        log.info("execute: " + String.join(" ", withBinaryCommand));
      }

      final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);

      pb.directory(new File(workDir));
      if (StringUtils.isNotEmpty(tolkStdLibPath)) {
        Map<String, String> env = pb.environment();
        env.put("TOLK_STDLIB", tolkStdLibPath);
      }
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
        throw new Exception("Error running " + withBinaryCommand);
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
