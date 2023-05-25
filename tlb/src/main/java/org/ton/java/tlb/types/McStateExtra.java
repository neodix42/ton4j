package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.TonHashMap;

@Builder
@Getter
@Setter
@ToString
public class McStateExtra {
    long magic; //   `tlb:"#cc26"`
    TonHashMap shardHashes; //*cell.Dictionary   `tlb:"dict 32"`
    ConfigParams configParams; //       `tlb:"."`
    Cell info; // *cell.Cell   `tlb:"^"`
    CurrencyCollection globalBalance;// `tlb:"."`
}
