package org.ton.java.func;

import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.utils.Utils;

@Builder
@Slf4j
public class FuncRunner {

  public String funcExecutablePath;

  public static String funcExecutable = "";

  public static class FuncRunnerBuilder {}

  public static FuncRunnerBuilder builder() {
    return new CustomFuncRunnerBuilder();
  }

  private static class CustomFuncRunnerBuilder extends FuncRunnerBuilder {

    @Override
    public FuncRunner build() {
      if (StringUtils.isEmpty(super.funcExecutablePath)) {
        log.info("Checking if Func is installed...");

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

          log.info("Func found at " + funcAbsolutePath);
          funcExecutable = "func";

        } catch (Exception e) {
          log.info(e.getMessage());
          throw new Error("Cannot execute simple Func command.\n" + errorMsg);
        }
      } else {
        log.info("using " + super.funcExecutablePath);
        funcExecutable = super.funcExecutablePath;
      }
      return super.build();
    }
  }

  public String run(String workdir, String... params) {
    Pair<Process, String> result = Executor.execute(funcExecutable, workdir, params);

    if (result != null && result.getRight() != null) {
      return result.getRight();
    }

    return "";
  }

  public String getFuncPath() {
    return Utils.detectAbsolutePath("func", false);
  }
}
