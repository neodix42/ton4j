package org.ton.ton4j.exporter.lazy;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.exporter.reader.CellDbReader;

/**
 * Port of C++ LabelParser from crypto/vm/dict.cpp Parses labels in Patricia tree (dictionary) nodes
 */
@Slf4j
@Getter
public class LabelParser {
  CellSliceLazy remainder;
  int lBits; // label length in bits
  int lOffs; // offset in bits (for non-same labels)
  int lSame; // for hml_same: bit value (0 or 1) in low bit, or 0 for non-same
  int sBits; // actual bits to skip in remainder (0 for l_same)

  /**
   * Constructor that parses label from a Cell
   *
   * @param cell The cell containing the label
   * @param maxLabelLen Maximum possible label length
   * @param labelMode Validation mode (0=none, 1=simple, 2=extended)
   */
  public LabelParser(CellDbReader cellDbReader, Cell cell, int maxLabelLen, int labelMode) {
    CellSliceLazy cs = CellSliceLazy.beginParse(cellDbReader, cell);
    if (!parseLabel(cs, maxLabelLen)) {
      lOffs = 0;
      lBits = 0;
    } else {
      sBits = (lSame != 0) ? 0 : lBits;
      remainder = cs;
    }

    if (labelMode > 0) {
      validate();
      if (labelMode >= 2) {
        validateSimple(maxLabelLen);
      }
    }
  }

  /**
   * Constructor that parses label from a CellSlice
   *
   * @param cs The cell slice containing the label
   * @param maxLabelLen Maximum possible label length
   * @param labelMode Validation mode
   */
  public LabelParser(CellSliceLazy cs, int maxLabelLen, int labelMode) {
    if (!parseLabel(cs, maxLabelLen)) {
      lOffs = 0;
      lBits = 0;
    } else {
      sBits = (lSame != 0) ? 0 : lBits;
      remainder = cs;
    }

    if (labelMode > 0) {
      validate();
      if (labelMode >= 2) {
        validateSimple(maxLabelLen);
      }
    }
  }

  /** Parse label from cell slice Returns true if successful */
  private boolean parseLabel(CellSliceLazy cs, int maxLabelLen) {
    int ltype = cs.preloadUint(2).intValue();

    switch (ltype) {
      case 0:
        {
          // hml_short$0
          lBits = 0;
          lOffs = 2;
          cs.skipBits(2);
          return true;
        }
      case 1:
        {
          // hml_short$0 with unary length
          cs.skipBits(1);
          lBits = countLeadingOnes(cs);
          if (lBits > maxLabelLen || cs.getRestBits() < 2 * lBits + 1) {
            return false;
          }
          lOffs = lBits + 2;
          cs.skipBits(lBits + 1);
          return true;
        }
      case 2:
        {
          // hml_long$10
          int lenBits = 32 - Integer.numberOfLeadingZeros(maxLabelLen);
          cs.skipBits(2);
          if (cs.getRestBits() < lenBits) {
            return false;
          }
          lBits = cs.loadUint(lenBits).intValue();
          if (lBits < 0 || lBits > maxLabelLen) {
            return false;
          }
          lOffs = lenBits + 2;
          return cs.getRestBits() >= lBits;
        }
      case 3:
        {
          // hml_same$11
          int lenBits = 32 - Integer.numberOfLeadingZeros(maxLabelLen);
          if (cs.getRestBits() < 3 + lenBits) {
            return false;
          }
          lSame = cs.loadUint(3).intValue();
          lBits = cs.loadUint(lenBits).intValue();
          if (lBits < 0 || lBits > maxLabelLen) {
            return false;
          }
          lOffs = -1;
          return true;
        }
      default:
        return false;
    }
  }

  /** Count leading 1 bits in cell slice */
  private int countLeadingOnes(CellSliceLazy cs) {
    int count = 0;
    while (count < cs.getRestBits() && cs.preloadBitAt(count)) {
      count++;
    }
    return count;
  }

  /**
   * Check if this label is a prefix of the given key
   *
   * @param key The key to check against
   * @param len Length of key in bits
   * @return true if label is a prefix of key
   */
  public boolean isPrefixOf(BitString key, int len) {
    if (lBits > len) {
      return false;
    } else if (lSame == 0) {
      // Non-same label: check if key starts with the label bits from remainder
      CellSliceLazy tempRemainder = remainder.clone();
      for (int i = 0; i < lBits; i++) {
        if (i >= key.getUsedBits() || key.get(i) != tempRemainder.loadBit()) {
          return false;
        }
      }
      return true;
    } else {
      // Same label: check if key starts with lBits of the same bit value
      boolean bitValue = (lSame & 1) != 0;
      for (int i = 0; i < lBits; i++) {
        if (i >= key.getUsedBits() || key.get(i) != bitValue) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Extract label bits to the given BitString
   *
   * @param to Destination for label bits
   * @return Number of bits extracted
   */
  public int extractLabelTo(BitString to) {
    if (lSame == 0) {
      // Copy bits from remainder
      for (int i = 0; i < lBits; i++) {
        to.writeBit(remainder.loadBit());
      }
    } else {
      // Fill with same bit value
      boolean bitValue = (lSame & 1) != 0;
      for (int i = 0; i < lBits; i++) {
        to.writeBit(bitValue);
      }
    }
    return lBits;
  }

  /** Skip the label in the remainder */
  public void skipLabel() {
    if (lSame == 0 && lBits > 0) {
      remainder.skipBits(lBits);
    }
  }

  /** Basic validation */
  private void validate() {
    if (lOffs == 0 && lBits == 0) {
      throw new RuntimeException("Error while parsing a dictionary node label");
    }
  }

  /** Simple validation with max length check */
  private void validateSimple(int n) {
    if (lBits > n) {
      throw new RuntimeException("Invalid dictionary node");
    } else if (lBits < n && (remainder.getRestBits() < sBits || remainder.getRefsCountLazy() < 2)) {
      throw new RuntimeException("Invalid dictionary fork node");
    }
  }

  /** Check if valid */
  public boolean isValid() {
    return lOffs != 0 || lBits != 0;
  }
}
