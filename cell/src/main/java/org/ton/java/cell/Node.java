package org.ton.java.cell;

import org.ton.java.bitstring.BitString;

public class Node {
    BitString key;
    Cell value;

    public Node(BitString key, Cell value) {
        this.key = key;
        this.value = value;
    }
}
