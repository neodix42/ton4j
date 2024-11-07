package org.ton.java.tolk;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
public class Executor {

  public static Pair<Process, String> execute(
      String pathToBinary, String workDir, String... command) {

    String[] withBinaryCommand = new String[] {pathToBinary};

    withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

    try {
      log.info("execute: " + String.join(" ", withBinaryCommand));

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
        log.info("exit value {}", p.exitValue());
        log.info(resultInput);
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
