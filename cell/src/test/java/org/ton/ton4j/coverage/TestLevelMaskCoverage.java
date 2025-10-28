package org.ton.ton4j.coverage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.cell.LevelMask;

@RunWith(JUnit4.class)
public class TestLevelMaskCoverage {

  @Test
  public void testBasics() {
    LevelMask m0 = new LevelMask(0);
    assertThat(m0.getLevel()).isEqualTo(0);
    assertThat(m0.getHashIndex()).isEqualTo(0);
    assertThat(m0.getHashesCount()).isEqualTo(1);
    assertThat(m0.isSignificant(0)).isTrue();
    assertThat(m0.isSignificant(1)).isFalse();

    LevelMask m1 = new LevelMask(0b0001);
    assertThat(m1.getLevel()).isEqualTo(1);
    assertThat(m1.getHashIndex()).isEqualTo(1);
    assertThat(m1.getHashesCount()).isEqualTo(2);
    assertThat(m1.isSignificant(1)).isTrue();

    LevelMask m5 = new LevelMask(0b0101);
    assertThat(LevelMask.calculateMinimumBits(0)).isEqualTo(0);
    assertThat(LevelMask.calculateMinimumBits(1)).isEqualTo(1);
    assertThat(LevelMask.calculateMinimumBits(8)).isEqualTo(4);
    assertThat(LevelMask.calculateOnesBits(0b0101)).isEqualTo(2);

    LevelMask clone = m5.clone();
    assertThat(clone.getLevel()).isEqualTo(m5.getLevel());
    assertThat(clone.getHashIndex()).isEqualTo(m5.getHashIndex());
  }
}
