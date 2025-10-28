package org.ton.ton4j.coverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.cell.CellSerializationInfo;
import org.ton.ton4j.cell.LevelMask;

@RunWith(JUnit4.class)
public class TestCellSerializationInfoCoverage {

  @Test
  public void testCreateNormalNoHashes() {
    // d1: refsNum=3 (0b011), special=0, withHashes=0, levelMask=0
    int d1 = 0b00000_0_0_011;
    // d2: dataLength = (d2 >> 1) + (d2 & 1)
    // choose d2=0b0000_0110 => dataLength=(3)+0=3, dataWithBits=false
    int d2 = 0b0000_0110;

    CellSerializationInfo info = CellSerializationInfo.create(d1, d2);
    assertThat(info.getRefsCount()).isEqualTo(3);
    assertThat(info.isSpecial()).isFalse();
    assertThat(info.isWithHashes()).isFalse();
    assertThat(info.getLevelMask()).isNotNull();
    assertThat(info.getLevelMask().getMask()).isEqualTo(0);
    // Offsets
    assertThat(info.getDataLength()).isEqualTo(3);
    assertThat(info.isDataWithBits()).isFalse();
    // refsOffset and endOffset should be consistent
    assertThat(info.getEndOffset()).isEqualTo(info.getRefsOffset() + 3 * (32 + 2 + 1));
  }

  @Test
  public void testCreateNormalWithHashesAndLevel() {
    // d1: refsNum=1, special=1, withHashes=1, levelMask=0b001 (=> 1)
    int d1 = (0b001 << 5) | (1 << 4) | (1 << 3) | 0b001;
    // d2: odd -> dataWithBits=true, dataLength=(n>>1)+(n&1)
    int d2 = 0b0000_0101; // (2)+1 = 3, with bit flag

    CellSerializationInfo info = CellSerializationInfo.create(d1, d2);
    assertThat(info.getRefsCount()).isEqualTo(1);
    assertThat(info.isSpecial()).isTrue();
    assertThat(info.isWithHashes()).isTrue();
    LevelMask lm = info.getLevelMask();
    assertThat(lm.getMask()).isEqualTo(0b001);
    assertThat(lm.getHashesCount()).isEqualTo(lm.getHashIndex() + 1);
    assertThat(info.getDataLength()).isEqualTo(3);
    assertThat(info.isDataWithBits()).isTrue();
  }

  @Test
  public void testCreateInvalidFirstByte() {
    // refsNum=5 (>4), withHashes=0 -> triggers "Invalid first byte"
    int d1 = (0 << 5) | (0 << 4) | (0 << 3) | 0b101; // 0b101 = 5
    int d2 = 0;
    assertThrows(Error.class, () -> CellSerializationInfo.create(d1, d2));
  }

  @Test
  public void testCreateAbsentCellsError() {
    // refsNum=7 and withHashes=1 => triggers "do not deserialize absent cells!"
    int d1 = (0 << 5) | (1 << 4) | (0 << 3) | 0b111; // withHashes=1, refsNum=7
    int d2 = 0;
    assertThrows(Error.class, () -> CellSerializationInfo.create(d1, d2));
  }
}
