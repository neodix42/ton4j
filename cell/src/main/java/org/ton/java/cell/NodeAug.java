package org.ton.java.cell;

import org.ton.java.bitstring.BitString;

public class NodeAug {
    BitString key;
    Cell value;
    Cell extra;

//    Cell extra;

    public NodeAug(BitString key, Cell value, Cell extra) {
        this.key = key;
        this.value = value;
        this.extra = extra;
    }
}
