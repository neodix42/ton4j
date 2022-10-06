package org.ton.java.cell;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;

@Slf4j
@RunWith(ConcurrentTestRunner.class)
public class TestConcurrentCellBuilder {

    @Test
    @ThreadCount(6)
    public void testConcurrentCellBuilder() {
        Cell c = CellBuilder.beginCell().storeUint(1, 8).endCell();
        log.info(c.bits.toBitString());
        Cell d = CellBuilder.beginCell().storeUint(2, 8).endCell();
        log.info(d.bits.toBitString());
    }
}
