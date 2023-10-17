package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.TonHashMapE;

@Builder
@Getter
@Setter
@ToString
public class McStateExtra {
    long magic;                 //      `tlb:"#cc26"`
    TonHashMapE shardHashes;     //      `tlb:"dict 32"`
    ConfigParams configParams;  //      `tlb:"."`
    Cell info;                  //      `tlb:"^"`
    CurrencyCollection globalBalance;// `tlb:"."`

    private String getMagic() {
        return Long.toHexString(magic);
    }
}
