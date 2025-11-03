package org.ton.ton4j.coverage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.NodeAug;

@RunWith(JUnit4.class)
public class TestNodeAugCoverage {

  @Test
  public void testNodeAugConstructorAndFields() throws Exception {
    BitString key = new BitString(4);
    key.writeBits("1010");
    Cell valueAndExtra = CellBuilder.beginCell().storeUint(1, 1).endCell();
    Cell forkExtra = CellBuilder.beginCell().storeUint(0, 1).endCell();

    NodeAug node = new NodeAug(key, valueAndExtra, forkExtra);
    assertThat(node).isNotNull();

    // Access fields via reflection to ensure coverage
    java.lang.reflect.Field fKey = NodeAug.class.getDeclaredField("key");
    java.lang.reflect.Field fVal = NodeAug.class.getDeclaredField("valueAndExtra");
    java.lang.reflect.Field fFork = NodeAug.class.getDeclaredField("forkExtra");
    fKey.setAccessible(true);
    fVal.setAccessible(true);
    fFork.setAccessible(true);

    BitString rk = (BitString) fKey.get(node);
    Cell rv = (Cell) fVal.get(node);
    Cell rf = (Cell) fFork.get(node);

    assertThat(rk.toBitString()).isEqualTo("1010");
    assertThat(rv.getBitLength()).isEqualTo(1);
    assertThat(rf.getBitLength()).isEqualTo(1);
  }
}
