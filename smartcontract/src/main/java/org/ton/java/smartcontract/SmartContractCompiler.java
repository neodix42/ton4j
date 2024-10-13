package org.ton.java.smartcontract;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import jdk.nashorn.internal.ir.annotations.Ignore;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.cell.Cell;
import org.ton.java.fift.FiftRunner;
import org.ton.java.func.FuncRunner;

/**
 * Make sure you have fift and func installed. See <a
 * href="https://github.com/ton-blockchain/packages">packages</a> for instructions.
 */
@Builder
@Getter
@Log
public class SmartContractCompiler {

  String contractPath;
  String contractAsResource;

  @Ignore private FiftRunner fiftRunner;

  @Ignore private FuncRunner funcRunner;

  public static class SmartContractCompilerBuilder {}

  public static SmartContractCompilerBuilder builder() {
    return new CustomSmartContractCompilerBuilder();
  }

  private static class CustomSmartContractCompilerBuilder extends SmartContractCompilerBuilder {
    @Override
    public SmartContractCompiler build() {
      super.funcRunner = FuncRunner.builder().build();
      super.fiftRunner = FiftRunner.builder().build();
      return super.build();
    }
  }

  /**
   * Compile to Boc in hex format
   *
   * @return code of BoC in hex
   */
  public String compile() {
    if (StringUtils.isNotEmpty(contractAsResource)) {
      try {
        URL resource = SmartContractCompiler.class.getResource(contractAsResource);
        contractPath = Paths.get(resource.toURI()).toFile().getAbsolutePath();
      } catch (Exception e) {
        throw new Error("Can't find resource " + contractAsResource);
      }
    }
    log.info("workdir " + new File(contractPath).getParent());

    String outputFiftAsmFile = funcRunner.run(new File(contractPath).getParent(), contractPath);

    if (outputFiftAsmFile.contains("cannot generate code")
        || outputFiftAsmFile.contains("error: undefined function")) {
      throw new Error("Compile error: " + outputFiftAsmFile);
    }
    //    outputFiftAsmFile = StringUtils.replace(outputFiftAsmFile, "2 boc+>B", "0 boc+>B");
    outputFiftAsmFile =
        "\"\"\"\"TonUtil.fif\"\"\"\" include \"\"\"\"Asm.fif\"\"\"\" include PROGRAM{ "
            + outputFiftAsmFile
            + "}END>c 2 boc+>B dup Bx.";
    outputFiftAsmFile =
        outputFiftAsmFile
            .replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "")
            .replaceAll("\n", " ")
            .replaceAll("\r", " ");
    return fiftRunner.runStdIn(new File(contractPath).getParent(), outputFiftAsmFile);
  }

  /**
   * Compile to Cell
   *
   * @return Cell
   */
  public Cell compileToCell() {
    return Cell.fromBoc(compile());
  }
}
