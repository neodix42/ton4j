package org.ton.ton4j.coverage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapAugE;
import org.ton.ton4j.cell.ValueExtra;

@RunWith(JUnit4.class)
public class TestTonHashMapAugECoverage {

  private static BitString key(String bits, int m) {
    BitString bs = new BitString(m);
    for (int i = 0; i < bits.length(); i++) bs.writeBit(bits.charAt(i) == '1');
    return bs;
  }

  private static final Function<Object, BitString> KEY = o -> key((String) o, 4);
  private static final Function<Object, Object> VALUE = o -> CellBuilder.beginCell().storeUint(1, 1).endCell();
  private static final Function<Object, Object> EXTRA = o -> CellBuilder.beginCell().storeUint(0, 1).endCell();
  private static final BiFunction<Object, Object, Object> FORK = (l, r) -> CellBuilder.beginCell().storeUint(1, 1).endCell();

  @Test
  public void testEmptyAndNonEmptySerialize() {
    TonHashMapAugE aug = new TonHashMapAugE(4);

    // empty -> first bit false
    Cell empty = aug.serialize(KEY, VALUE, EXTRA, FORK);
    CellSlice se = CellSlice.beginParse(empty);
    assertThat(se.loadBit()).isFalse();

    // non-empty -> first bit true, then a ref to dict cell
    aug.getElements().put("0001", new ValueExtra("v", "e"));
    Cell nonEmpty = aug.serialize(KEY, VALUE, EXTRA, FORK);
    CellSlice sn = CellSlice.beginParse(nonEmpty);
    assertThat(sn.loadBit()).isTrue();
    assertThat(sn.loadRef().print()).contains("x{");
  }
}
