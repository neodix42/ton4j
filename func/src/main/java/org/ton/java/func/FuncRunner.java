package org.ton.java.func;

import java.util.concurrent.TimeUnit;
import jdk.nashorn.internal.ir.annotations.Ignore;
import lombok.Builder;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.utils.Utils;

@Builder
@Log
public class FuncRunner {

  String funcExecutablePath;

  @Ignore private String funcExecutable;

  public static class FuncRunnerBuilder {}

  public static FuncRunnerBuilder builder() {
    return new CustomFuncRunnerBuilder();
  }

  private static class CustomFuncRunnerBuilder extends FuncRunnerBuilder {

    @Override
    public FuncRunner build() {
      if (StringUtils.isEmpty(super.funcExecutablePath)) {
        log.info("checking if func is installed...");

        String errorMsg =
            "Make sure you have func installed. See https://github.com/ton-blockchain/packages for instructions.\nYou can also specify full path via SmartContractCompiler.funcExecutablePath().";
        try {
          ProcessBuilder pb = new ProcessBuilder("func", "-h").redirectErrorStream(true);
          Process p = pb.start();
          p.waitFor(1, TimeUnit.SECONDS);
          if (p.exitValue() != 2) {
            throw new Error("Cannot execute simple func command.\n" + errorMsg);
          }
          String funcAbsolutePath = Utils.detectAbsolutePath("func", false);

          log.info("func found at " + funcAbsolutePath);
          super.funcExecutable = "func";

        } catch (Exception e) {
          log.info(e.getMessage());
          throw new Error("Cannot execute simple func command.\n" + errorMsg);
        }
      } else {
        log.info("using " + super.funcExecutablePath);
        super.funcExecutable = super.funcExecutablePath;
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
}
