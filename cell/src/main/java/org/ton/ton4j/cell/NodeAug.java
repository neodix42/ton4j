package org.ton.ton4j.cell;

import org.ton.ton4j.bitstring.BitString;

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
