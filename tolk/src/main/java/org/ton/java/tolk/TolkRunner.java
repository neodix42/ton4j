package org.ton.java.tolk;

import static java.util.Objects.isNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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
public class TolkRunner {

  public String tolkExecutablePath;
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
            "Make sure you have Tolk installed. See https://github.com/ton-blockchain/packages for instructions.\nYou can also specify full path via SmartContractCompiler.tolkExecutablePath().";
        try {
          ProcessBuilder pb = new ProcessBuilder("tolk", "-h").redirectErrorStream(true);
          Process p = pb.start();
          p.waitFor(1, TimeUnit.SECONDS);
          if (p.exitValue() != 2) {
            throw new Error("Cannot execute simple Tolk command.\n" + errorMsg);
          }
          String tolkAbsolutePath = Utils.detectAbsolutePath("tolk", false);

          if (super.printInfo) {
            log.info("Tolk found at " + tolkAbsolutePath);
          }
          tolkExecutable = "tolk";

        } catch (Exception e) {
          log.info(e.getMessage());
          throw new Error("Cannot execute simple Tolk command.\n" + errorMsg);
        }
      } else {
        if (super.printInfo) {
          log.info("using " + super.tolkExecutablePath);
        }
        tolkExecutable = super.tolkExecutablePath;
      }
      return super.build();
    }
  }

  public String run(String workdir, String... params) {
    Pair<Process, String> result = execute(tolkExecutable, workdir, params);

    if (result != null && result.getRight() != null) {
      return result.getRight();
    }

    return "";
  }

  public String getTolkPath() {
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
