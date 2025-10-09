package org.ton.ton4j.cell;

import java.io.Serializable;
import lombok.Data;
import org.ton.ton4j.bitstring.BitString;

@Data
public class Node implements Serializable {
  BitString key;
  Cell value;

  public Node(BitString key, Cell value) {
    this.key = key;
    this.value = value;
  }
}
