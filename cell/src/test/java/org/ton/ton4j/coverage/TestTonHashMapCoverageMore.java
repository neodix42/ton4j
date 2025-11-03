package org.ton.ton4j.coverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.CellType;
import org.ton.ton4j.cell.TonHashMap;

@RunWith(JUnit4.class)
public class TestTonHashMapCoverageMore {

  private static BitString bsFrom(String bits, int keySize) {
    BitString bs = new BitString(keySize);
    for (int i = 0; i < bits.length(); i++) {
      bs.writeBit(bits.charAt(i) == '1');
    }
    return bs;
  }

  @Test
  public void testDeserializeLabelShortLongSame() {
    TonHashMap dict = new TonHashMap(4);

    // Short: first bit 0, then unary n=1 -> '10', then s='1'
    CellBuilder bs1 = CellBuilder.beginCell();
    bs1.storeBit(false); // short selector
    bs1.storeBit(true); // unary 1
    bs1.storeBit(false); // unary stop 0
    bs1.storeBit(true); // s bit '1'
    BitString decoded1 = dict.deserializeLabel(CellSlice.beginParse(bs1.endCell()), 4);
    assertThat(decoded1.toBitString()).isEqualTo("1");

    // Long: '10' selector, then n=4 in sizeOfM=3 bits -> '100', then s='1010'
    CellBuilder bs2 = CellBuilder.beginCell();
    bs2.storeBit(true);
    bs2.storeBit(false);
    bs2.storeUint(4, 3);
    bs2.storeBits("1010");
    BitString decoded2 = dict.deserializeLabel(CellSlice.beginParse(bs2.endCell()), 4);
    assertThat(decoded2.toBitString()).isEqualTo("1010");

    // Same: '11' selector, v=0, n=3 in 3 bits -> '011'
    CellBuilder bs3 = CellBuilder.beginCell();
    bs3.storeBit(true);
    bs3.storeBit(true);
    bs3.storeBit(false); // v=0
    bs3.storeUint(3, 3);
    BitString decoded3 = dict.deserializeLabel(CellSlice.beginParse(bs3.endCell()), 4);
    assertThat(decoded3.toBitString()).isEqualTo("000");
  }

  @Test
  public void testSerializeEmptyThrows() {
    TonHashMap dict = new TonHashMap(4);
    assertThatThrownBy(() -> dict.serialize(k -> bsFrom("0", 4), v -> CellBuilder.beginCell().endCell()))
        .isInstanceOf(Error.class)
        .hasMessageContaining("does not support empty dict");
  }

  @Test
  public void testSerializeAndBuildMerkleProofCoversPruned() {
    TonHashMap dict = new TonHashMap(4);
    // Fill elements directly
    dict.getElements().put("0000", CellBuilder.beginCell().storeUint(1, 1).endCell());
    dict.getElements().put("1111", CellBuilder.beginCell().storeUint(0, 1).endCell());

    Function<Object, BitString> keySer = o -> bsFrom((String) o, 4);
    Function<Object, Cell> valSer = o -> (Cell) o;

    Cell tree = dict.serialize(keySer, valSer);
    assertThat(tree.getCellType()).isEqualTo(CellType.ORDINARY);

    // Build merkle proof for one of keys to hit convertToMerkleProof and convertToPrunedBranch
    Cell proof = dict.buildMerkleProof("0000", keySer, valSer);
    assertThat(proof.getCellType()).isEqualTo(CellType.MERKLE_PROOF);

    // The inner ref should be pruned/ordinary mix; ensure printing works
    assertThat(proof.print()).startsWith("p{");
  }

  @Test
  public void testGetKeyValueByIndexAndErrors() {
    TonHashMap dict = new TonHashMap(4);
    dict.getElements().put("k1", "v1");
    dict.getElements().put("k2", "v2");

    Object k0 = dict.getKeyByIndex(0);
    Object v1 = dict.getValueByIndex(1);
    assertThat(k0).isIn("k1", "k2");
    assertThat(v1).isIn("v1", "v2");

    // error paths
    assertThatThrownBy(() -> dict.getKeyByIndex(-1)).isInstanceOf(Error.class);
    assertThatThrownBy(() -> dict.getValueByIndex(5)).isInstanceOf(Error.class);
  }
}
