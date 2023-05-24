package org.ton.java.tlb;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.loader.Tlb;
import org.ton.java.tlb.types.StateUpdate;

@Slf4j
@RunWith(JUnit4.class)
public class TestTlbLoader {

    @Test
    public void testBlockNotMaster() {
        System.out.println("class " + CellSlice.class.getSimpleName());
        CellSlice cs = CellSlice.beginParse(CellBuilder.beginCell()
                .storeUint(33, 8)
                .storeUint(125, 64)
                .endCell());
        StateUpdate su = (StateUpdate) Tlb.load(StateUpdate.class, cs);

    }
}
