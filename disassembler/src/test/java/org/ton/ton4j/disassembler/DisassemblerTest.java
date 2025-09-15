package org.ton.ton4j.disassembler;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.fift.FiftRunner;
import org.ton.ton4j.func.FuncRunner;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.FullAccountState;
import org.ton.ton4j.utils.Utils;

@RunWith(JUnit4.class)
public class DisassemblerTest {
  static String tonlibPath = Utils.getTonlibGithubUrl();
  static String funcPath = Utils.getFuncGithubUrl();
  static String fiftPath = Utils.getFiftGithubUrl();

  private static final String BASE_DIR = "src/test/resources/";
  private static final String BOC_DIR = BASE_DIR + "bocs/";
  private static final String TXT_DIR = BASE_DIR + "snapshots/";

  private static Tonlib tonlib =
      Tonlib.builder().pathToTonlibSharedLib(tonlibPath).testnet(false).ignoreCache(false).build();

  @Test
  public void shouldDisassembleConfig() throws Exception {
    byte[] boc = fetchCodeOrSnapshot("Ef9VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVbxn");
    String result = normalizeLines(Disassembler.fromBoc(boc));
    String actual = loadSnapshot("config");

    assertThat(actual).isEqualToNormalizingNewlines(result);
  }

  @Ignore
  @Test
  public void shouldDisassembleAnyConfig() {
    Tonlib.builder()
        .pathToTonlibSharedLib(tonlibPath)
        .pathToGlobalConfig(Utils.getGlobalConfigUrlMyLocalTon())
        .ignoreCache(false)
        .build();
    Address address = Address.of("Ef9VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVbxn");
    FullAccountState accountState = tonlib.getAccountState(address);
    byte[] codeBoc = Utils.base64ToBytes(accountState.getAccount_state().getCode());
    String result = normalizeLines(Disassembler.fromBoc(codeBoc));
    System.out.println(result);
  }

  @Test
  public void shouldDisassembleNft() throws Exception {
    byte[] boc = fetchCodeOrSnapshot("EQBmG4YwsdGsUHG46rL-_GtGxsUrdmn-8Tau1DKkzQMNsGaW");
    String result = normalizeLines(Disassembler.fromBoc(boc));
    assertThat(loadSnapshot("nft")).isEqualToNormalizingNewlines(result);
  }

  @Test
  public void shouldDumpMethod() throws Exception {
    String code = loadFiftFromFunc();
    Cell codeCell = Cell.fromBoc(code);
    String result = normalizeLines(Disassembler.fromCode(codeCell));
    assertThat(loadSnapshot("dump")).isEqualToNormalizingNewlines(result);
  }

  @Test
  public void shouldDisassembleElector() throws Exception {
    byte[] boc = fetchCodeOrSnapshot("Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF");
    String result = normalizeLines(Disassembler.fromBoc(boc));
    assertThat(loadSnapshot("elector")).isEqualToNormalizingNewlines(result);
  }

  @Test
  public void shouldDisassembleContract() throws Exception {
    byte[] boc = fetchCodeOrSnapshot("EQBRrTk63wHpvreMs7_cDKWh6zrYmQcSBOjKz1i6GcbRTLZX");
    String result = normalizeLines(Disassembler.fromBoc(boc));
    assertThat(loadSnapshot("contract")).isEqualToNormalizingNewlines(result);
  }

  @Test
  public void shouldDisassembleNumber5() throws Exception {
    byte[] boc = fetchCodeOrSnapshot("EQDSbgHX03B9_0cNBAMdlmhVbvhRNYhZNhTRH4wfNBmisKB5");
    String result = normalizeLines(Disassembler.fromBoc(boc));
    assertThat(loadSnapshot("numberFive")).isEqualToNormalizingNewlines(result);
  }

  private byte[] fetchCodeOrSnapshot(String addr) throws Exception {
    Path bocPath = Paths.get(BOC_DIR, addr + ".boc");
    if (Files.exists(bocPath)) {
      return Files.readAllBytes(bocPath);
    }

    Address address = Address.of(addr);
    FullAccountState accountState = tonlib.getAccountState(address);
    byte[] code = Utils.base64ToBytes(accountState.getAccount_state().getCode());

    Files.createDirectories(Paths.get(BOC_DIR));
    Files.write(bocPath, code);

    return code;
  }

  private String loadSnapshot(String addr) throws IOException {
    Path snapshotPath = Paths.get(TXT_DIR, addr + ".txt");
    if (Files.exists(snapshotPath)) {
      byte[] bytes = Files.readAllBytes(snapshotPath);
      return new String(bytes, StandardCharsets.UTF_8);
    } else {
      throw new IOException("Snapshot not found: " + snapshotPath);
    }
  }

  private String loadFiftFromFunc() throws IOException {
    String rawInstruction =
        "    () main() {\n"
            + "\n"
            + "    }\n"
            + "\n"
            + "    () owner() method_id {\n"
            + "\n"
            + "    }";

    String funcFileName = "test.fc";
    File funcFile = Paths.get(BASE_DIR + funcFileName).toFile();

    if (Files.notExists(funcFile.toPath())) {
      Files.write(funcFile.toPath(), rawInstruction.getBytes());
    }

    FuncRunner funcRunner = FuncRunner.builder().funcExecutablePath(funcPath).build();
    String fiftCode = funcRunner.run(funcFile.getParent(), "-PA", funcFile.getAbsolutePath());

    String fiftFileName = "test.fift";
    File fiftFile = Paths.get(BASE_DIR + fiftFileName).toFile();

    if (Files.notExists(fiftFile.toPath())) {
      Files.write(fiftFile.toPath(), fiftCode.getBytes());
      if (!fiftCode.contains("2 boc+>B")) {
        Files.write(fiftFile.toPath(), "2 boc+>B dup Bx.".getBytes(), StandardOpenOption.APPEND);
      }
    }

    FiftRunner fiftRunner = FiftRunner.builder().fiftExecutablePath(fiftPath).build();

    return fiftRunner.run(fiftFile.getParent(), "-s", fiftFile.getAbsolutePath());
  }

  // visually exactly the same lines would be generated
  // normalizes default file formatting
  private String normalizeLines(String s) {
    return s.replaceAll("\r", "");
  }
}
