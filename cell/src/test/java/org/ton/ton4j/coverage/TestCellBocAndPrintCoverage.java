package org.ton.ton4j.coverage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellType;

@RunWith(JUnit4.class)
public class TestCellBocAndPrintCoverage {

  private Cell makeSimpleCell() {
    return CellBuilder.beginCell().storeUint(0xAB, 8).storeUint(0xCD, 8).endCell();
  }

  //
  //  @Test
  //  public void testToBocVariantsAndRoundTrip() {
  //    Cell c = makeSimpleCell();
  //
  //    // All combinations of flags
  //    boolean[] tf = new boolean[] {false, true};
  //    for (boolean crc : tf) {
  //      for (boolean idx : tf) {
  //        for (boolean cache : tf) {
  //          for (boolean topHash : tf) {
  //            for (boolean intHashes : tf) {
  //              byte[] boc = c.toBoc(crc, idx, cache, topHash, intHashes);
  //              Cell r = CellBuilder.beginCell().fromBoc(boc).endCell();
  //              // Verify content is intact
  //              CellSlice cs = CellSlice.beginParse(r);
  //              assertThat(cs.loadUint(8).intValue()).isEqualTo(0xAB);
  //              assertThat(cs.loadUint(8).intValue()).isEqualTo(0xCD);
  //              cs.endParse();
  //            }
  //          }
  //        }
  //      }
  //    }
  //  }

  @Test
  public void testToHexAndBase64Helpers() {
    Cell c = makeSimpleCell();
    // hex overloads
    assertThat(c.toHex(true)).isNotEmpty();
    assertThat(c.toHex(false)).isNotEmpty();
    assertThat(c.toHex(true, true)).isNotEmpty();
    assertThat(c.toHex(true, true, true)).isNotEmpty();
    assertThat(c.toHex(true, false, true, false)).isNotEmpty();
    assertThat(c.toHex(true, false, true, false, true)).isNotEmpty();

    // defaults
    assertThat(c.toHex()).isNotEmpty();
    assertThat(c.toBase64()).isNotEmpty();
    assertThat(c.toBase64UrlSafe()).isNotEmpty();
    assertThat(c.toBase64(false)).isNotEmpty();

    // bitstring helpers
    assertThat(c.bitStringToHex()).isNotEmpty();
    assertThat(c.toBitString()).isNotEmpty();
  }

  @Test
  public void testPrintForTypes() {
    // ordinary default prints with 'x'
    Cell cOrd = makeSimpleCell();
    assertThat(cOrd.print()).startsWith("x{");

    // Create exotic types and verify prefix
    Cell cP = CellBuilder.beginCell().cellType(CellType.PRUNED_BRANCH).setExotic(true).endCell();
    assertThat(cP.print()).startsWith("P{");

    Cell cProof =
        CellBuilder.beginCell()
            .cellType(CellType.MERKLE_PROOF)
            .setExotic(true)
            .storeRef(CellBuilder.beginCell().endCell())
            .endCell();
    assertThat(cProof.print()).startsWith("p{");

    Cell cUpd =
        CellBuilder.beginCell()
            .cellType(CellType.MERKLE_UPDATE)
            .setExotic(true)
            .storeRef(CellBuilder.beginCell().endCell())
            .storeRef(CellBuilder.beginCell().endCell())
            .endCell();
    assertThat(cUpd.print()).startsWith("u{");

    // Verify nested printing indents recursively
    Cell parent = CellBuilder.beginCell().storeRef(cOrd).storeRef(cP).endCell();
    String printed = parent.print("");
    // Expect multiple lines and both x and P prefixes
    assertThat(printed).contains("x{");
    assertThat(printed).contains("P{");
  }

  @Test
  public void testToBocMultiRootIndexing() {
    // Build small tree with shared child to cover flattenIndex repeat path
    Cell leaf = CellBuilder.beginCell().storeUint(1, 1).endCell();
    Cell a = CellBuilder.beginCell().storeRef(leaf).endCell();
    Cell b = CellBuilder.beginCell().storeRef(leaf).endCell();
    byte[] boc = a.toBocMultiRoot(Arrays.asList(a, b), true, true, true, true, true);
    assertThat(boc.length).isGreaterThan(0);
  }
}
