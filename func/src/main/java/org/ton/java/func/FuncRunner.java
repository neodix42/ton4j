package org.ton.java.func;

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
public class FuncRunner {

  public String funcExecutablePath;
  private Boolean printInfo;
  public static String funcExecutable = "";

  public static class FuncRunnerBuilder {}

  public static FuncRunnerBuilder builder() {
    return new CustomFuncRunnerBuilder();
  }

  private static class CustomFuncRunnerBuilder extends FuncRunnerBuilder {

    @Override
    public FuncRunner build() {
      if (isNull(super.printInfo)) {
        super.printInfo = true;
      }
      if (StringUtils.isEmpty(super.funcExecutablePath)) {
        if (super.printInfo) {
          log.info("Checking if Func is installed...");
        }

        String errorMsg =
            "Make sure you have Func installed. See https://github.com/ton-blockchain/packages for instructions.\nYou can also specify full path via SmartContractCompiler.funcExecutablePath().";
        try {
          ProcessBuilder pb = new ProcessBuilder("func", "-h").redirectErrorStream(true);
          Process p = pb.start();
          p.waitFor(1, TimeUnit.SECONDS);
          if (p.exitValue() != 2) {
            throw new Error("Cannot execute simple Func command.\n" + errorMsg);
          }
          String funcAbsolutePath = Utils.detectAbsolutePath("func", false);

          if (super.printInfo) {
            log.info("Func found at " + funcAbsolutePath);
          }
          funcExecutable = "func";

        } catch (Exception e) {
          log.error(e.getMessage());
          throw new Error("Cannot execute simple Func command.\n" + errorMsg);
        }
      } else {
        super.funcExecutablePath = Utils.download(super.funcExecutablePath);
        if (super.printInfo) {
          log.info("using " + super.funcExecutablePath);
        }
        funcExecutable = super.funcExecutablePath;
      }
      return super.build();
    }
  }

  public String run(String workdir, String... params) {
    Pair<Process, String> result = execute(funcExecutable, workdir, params);

    if (result != null && result.getRight() != null) {
      return result.getRight();
    }

    return "";
  }

  public String getFuncPath() {
    return Utils.detectAbsolutePath("func", false);
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
