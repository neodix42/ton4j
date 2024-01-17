package org.ton.java.cell;

import org.ton.java.bitstring.BitString;

public class NodeAug {
    BitString key;
    Cell valueAndExtra;
    Cell forkExtra;

    public NodeAug(BitString key, Cell valueAndExtra, Cell forkExtra) {
        this.key = key;
        this.valueAndExtra = valueAndExtra;
        this.forkExtra = forkExtra;
    }
}
