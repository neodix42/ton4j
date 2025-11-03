package org.ton.ton4j.coverage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.TonHashMap;

@RunWith(JUnit4.class)
public class TestTonHashMapCoverageSplitFlatten {

  private static BitString key(String bits, int m) {
    BitString bs = new BitString(m);
    for (int i = 0; i < bits.length(); i++) bs.writeBit(bits.charAt(i) == '1');
    return bs;
  }

  private static final Function<Object, BitString> KEY4 = o -> key((String) o, 4);
  private static final Function<Object, Cell> VAL = o -> CellBuilder.beginCell().storeUint(1, 1).endCell();

  @Test
  public void testSplitTreeFlattenBothBranchesAndMerkleProofLeftRight() {
    TonHashMap dict = new TonHashMap(4);
    // Create three entries to force splits and both left/right-null cases during flatten
    dict.getElements().put("0000", VAL.apply(null));
    dict.getElements().put("1111", VAL.apply(null));
    dict.getElements().put("1000", VAL.apply(null));

    // Serialize triggers splitTree + flatten
    Cell tree = dict.serialize(KEY4, VAL);
    assertThat(tree).isNotNull();

    // Build merkle proof for a left-leaning key and a right-leaning key to exercise recursion paths
    Cell proofLeft = dict.buildMerkleProof("0000", KEY4, VAL);
    Cell proofRight = dict.buildMerkleProof("1111", KEY4, VAL);

    assertThat(proofLeft.print()).startsWith("p{");
    assertThat(proofRight.print()).startsWith("p{");

    // Access last index to cover array conversion in getKeyByIndex/getValueByIndex
    Object lastKey = dict.getKeyByIndex(dict.getElements().size() - 1);
    Object lastVal = dict.getValueByIndex(dict.getElements().size() - 1);
    assertThat(lastKey).isIn("0000", "1111", "1000");
    assertThat(lastVal).isInstanceOfAny(Cell.class);
  }
}
