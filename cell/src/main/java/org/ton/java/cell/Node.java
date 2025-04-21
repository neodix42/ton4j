package org.ton.java.cell;

import java.io.Serializable;
import org.ton.java.bitstring.BitString;

public class Node implements Serializable {
  BitString key;
  Cell value;

  public Node(BitString key, Cell value) {
    this.key = key;
    this.value = value;
  }
}
