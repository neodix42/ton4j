package org.ton.java.cell;

import java.io.Serializable;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class LevelMask implements Serializable {

  int mask;
  int level;

  int hashIndex;

  public LevelMask(int mask) {
    this.mask = mask;
    this.level = getLevel();
    this.hashIndex = getHashIndex();
  }

  public LevelMask clone() {
    return new LevelMask(mask);
  }

  public int getLevel() {
    return calculateMinimumBits(mask);
  }

  public LevelMask apply(int lvl) {
    return new LevelMask(mask & ((1 << lvl) - 1));
  }

  public int getHashIndex() {
    return calculateOnesBits(mask);
  }

  public static int calculateMinimumBits(int number) {
    if (number == 0) {
      return 0;
    }

    return Integer.SIZE - Integer.numberOfLeadingZeros(number);
  }

  public static int calculateOnesBits(int number) {
    return Integer.bitCount(number);
  }

  public boolean isSignificant(int level) {
    return (level == 0) || ((mask >> (level - 1)) % 2 != 0);
  }
}
