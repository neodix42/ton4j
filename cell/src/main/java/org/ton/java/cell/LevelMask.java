package org.ton.java.cell;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class LevelMask {

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
            return 1;
        }

        return (int) Math.ceil(Math.log(number + 1) / Math.log(2));
    }

    public static int calculateOnesBits(int number) {
        return Integer.bitCount(number);
    }

    public boolean isSignificant(int level) {
        return (level == 0) || ((mask >> (level - 1)) % 2 != 0);
    }
}
