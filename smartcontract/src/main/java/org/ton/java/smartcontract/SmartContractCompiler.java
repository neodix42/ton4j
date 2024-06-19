package org.ton.java.smartcontract;

import jdk.nashorn.internal.ir.annotations.Ignore;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Make sure you have fift and func installed. See <a href="https://github.com/ton-blockchain/packages">packages</a> for instructions.
 */
@Builder
@Getter
@Log
public class SmartContractCompiler {

    String contractPath;
    String funcExecutablePath;
    String fiftExecutablePath;
    String fiftAsmLibraryPath;
    String fiftSmartcontLibraryPath;

    @Ignore
    private FiftRunner fiftRunner;

    @Ignore
    private FuncRunner funcRunner;


    public static class SmartContractCompilerBuilder {
    }

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
     * @return code of BoC in hex
     */
    public String compile() throws IOException {
        System.out.println("workdir " + new File(contractPath).getParent());

        String outputFiftAsmFile = funcRunner.run(new File(contractPath).getParent(), "-W", "dummy.boc", contractPath);

        if (!outputFiftAsmFile.contains("2 boc+>B")
                || outputFiftAsmFile.contains("cannot generate code")
                || outputFiftAsmFile.contains("error: undefined function")) {
            throw new Error("Compile error: " + outputFiftAsmFile);
        }
        outputFiftAsmFile = StringUtils.replace(outputFiftAsmFile, "2 boc+>B", "0 boc+>B");
        File file = new File(new File(contractPath).getParent() + "/dummy.fif");

        FileUtils.writeStringToFile(file, outputFiftAsmFile, Charset.defaultCharset());

        // output binary boc file
        fiftRunner.run(file.getParent(), "-s", file.getAbsolutePath());

        byte[] bocContent = FileUtils.readFileToByteArray(new File(file.getParent() + "/dummy.boc"));
        return Utils.bytesToHex(bocContent);
    }
}