package org.ton.java.tlb;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.loader.Tlb;
import org.ton.java.tlb.types.Text;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestTlbTextReader {
    @Test
    public void testLoadText() {

        String s = "Hello garage,Hello garage,Hello garage,Hello garage,Hello garage,Hello garage,Hello garage,Hello garage,Hello garage,Hello garage";

        Text txt = Text.builder()
                .maxFirstChunkSize(30)
                .value(s)
                .build();

        Cell c = txt.toCell();
        log.info("txtCell {}", c);

        Text loadedTxt = (Text) Tlb.load(Text.class, CellSlice.beginParse(c));
        log.info("loadedTxt {}", loadedTxt);
        assertThat(loadedTxt.getValue()).isEqualTo(txt.getValue());
    }
}