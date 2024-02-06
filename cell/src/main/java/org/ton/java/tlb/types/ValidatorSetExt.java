package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMap;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class ValidatorSetExt {
    long magic;           //Magic            `tlb:"#12"`
    long uTimeSince;      //uint32           `tlb:"## 32"`
    long uTimeUntil;      //uint32           `tlb:"## 32"`
    int total;            //uint16           `tlb:"## 16"`
    int main;             //uint16           `tlb:"## 16"`
    BigInteger totalWeight; //uint64         `tlb:"## 64"`
    TonHashMap list;         //*cell.Dictionary `tlb:"dict 16"`

    public static ValidatorSetExt deserialize(CellSlice cs) {
        return null;
    }
}
