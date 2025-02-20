package org.ton.java.tlb;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

@Slf4j
@RunWith(JUnit4.class)
public class TestTlbTextReader {
  @Test
  public void testLoadText() {

    String s =
        "Hello garage,Hello garage,Hello garage,Hello garage,Hello garage,Hello garage,Hello garage,Hello garage,Hello garage,Hello garage";

    Text txt = Text.builder().maxFirstChunkSize(30).value(s).build();

    Cell c = txt.toCell();
    log.info("txtCell {}", c);

    Text loadedTxt = Text.deserialize(CellSlice.beginParse(c));
    log.info("loadedTxt {}", loadedTxt);
    assertThat(loadedTxt.getValue()).isEqualTo(txt.getValue());
  }
}
