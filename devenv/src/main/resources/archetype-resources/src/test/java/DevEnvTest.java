import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.SmartContractCompiler;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;

@Slf4j
@RunWith(JUnit4.class)
public class DevEnvTest {

  static Cell codeCell;

  @BeforeClass
  public static void setUpBeforeClass() throws IOException {
    log.info("Compiling main.fc...");
    SmartContractCompiler smcFunc =
        SmartContractCompiler.builder().contractAsResource("/main.fc").build();
    codeCell = smcFunc.compileToCell();
    log.info("codeCell {}", codeCell.print());
  }

  @Test
  public void testCompileContract() throws IOException {}

  @Test
  public void testTonlibGetLastBlock() {
    Tonlib tonlib = Tonlib.builder().testnet(true).build();
    BlockIdExt block = tonlib.getLast().getLast();
    log.info("block {}", block);
  }
}
