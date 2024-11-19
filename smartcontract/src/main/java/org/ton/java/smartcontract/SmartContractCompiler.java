package org.ton.java.smartcontract;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.cell.Cell;
import org.ton.java.fift.FiftRunner;
import org.ton.java.func.FuncRunner;
import org.ton.java.tolk.TolkRunner;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;

import static java.util.Objects.isNull;

/**
 * Make sure you have fift and func installed. See <a
 * href="https://github.com/ton-blockchain/packages">packages</a> for instructions.
 */
@Builder
@Data
@Slf4j
public class SmartContractCompiler {

    String contractPath;
    String contractAsResource;

    private FiftRunner fiftRunner;

    private FuncRunner funcRunner;

    private TolkRunner tolkRunner;

    private Boolean printInfo;

    private boolean printFiftAsmOutput;

    public static class SmartContractCompilerBuilder {
    }

    public static SmartContractCompilerBuilder builder() {
        return new CustomSmartContractCompilerBuilder();
    }

    private static class CustomSmartContractCompilerBuilder extends SmartContractCompilerBuilder {
        @Override
        public SmartContractCompiler build() {
            if (isNull(super.printInfo)) {
                super.printInfo = true;
            }
            if (isNull(super.funcRunner)) {
                super.funcRunner = FuncRunner.builder().build();
            }
            if (isNull(super.fiftRunner)) {
                super.fiftRunner = FiftRunner.builder().build();
            }
            if (isNull(super.tolkRunner)) {
                super.tolkRunner = TolkRunner.builder().build();
            }
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
                URL resource = SmartContractCompiler.class.getClassLoader().getResource(contractAsResource);
                contractPath = Paths.get(resource.toURI()).toFile().getAbsolutePath();
            } catch (Exception e) {
                throw new Error("Can't find resource " + contractAsResource);
            }
        }
        if (isNull(printInfo)) {
            log.info("workdir " + new File(contractPath).getParent());
        }

        String outputFiftAsmFile;
        if (contractPath.contains(".func") || contractPath.contains(".fc")) {
            outputFiftAsmFile = funcRunner.run(new File(contractPath).getParent(), contractPath);
            // add missing includes, PROGRAM and to boc conversion

            outputFiftAsmFile =
                    "\"TonUtil.fif\" include \"Asm.fif\" include PROGRAM{ "
                            + outputFiftAsmFile
                            + "}END>c 2 boc+>B dup Bx.";
            outputFiftAsmFile =
                    outputFiftAsmFile
                            .replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "")
                            .replaceAll("\n", " ")
                            .replaceAll("\r", " ");

        } else { // tolk
            outputFiftAsmFile = tolkRunner.run(new File(contractPath).getParent(), contractPath);

            outputFiftAsmFile = "\"TonUtil.fif\" include " + outputFiftAsmFile + " 2 boc+>B dup Bx.";

            outputFiftAsmFile =
                    outputFiftAsmFile
                            .replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "")
                            .replaceAll("\n", " ")
                            .replaceAll("\r", " ");
        }

        if (outputFiftAsmFile.contains("cannot generate code")
                || outputFiftAsmFile.contains(": error:")
                || outputFiftAsmFile.contains("Failed to discover")) {
            throw new Error("Compile error: " + outputFiftAsmFile);
        }

        if (printFiftAsmOutput) {
            log.info(outputFiftAsmFile);
        }

        String result;
        try {
            File fiftFile = new File(contractPath + ".fif");
            FileUtils.writeStringToFile(fiftFile, outputFiftAsmFile, Charset.defaultCharset());
            result = fiftRunner.run(new File(contractPath).getParent(), "-s", fiftFile.getAbsolutePath());
            FileUtils.deleteQuietly(fiftFile);
            return result;
        } catch (Exception e) {
            throw new Error("Cannot compile " + contractPath + ", error " + e.getMessage());
        }
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
