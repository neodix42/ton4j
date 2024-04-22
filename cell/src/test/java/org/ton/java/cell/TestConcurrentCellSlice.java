package org.ton.java.cell;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(ConcurrentTestRunner.class)
public class TestConcurrentCellSlice {

    @Test
    @ThreadCount(6)
    public void testConcurrentCellSlice() {
        Cell c1 = CellBuilder.beginCell().fromBoc("b5ee9c72410101010003000001558501ef11").endCell();
        CellSlice cs = CellSlice.beginParse(c1);
        BigInteger i = cs.loadUint(7);
        assertThat(i.longValue()).isEqualTo(42);
        cs.endParse();

        Cell c2 = CellBuilder.beginCell().fromBoc("B5EE9C7241010101002200003F000000000000000000000000000000000000000000000000000000000000009352A2F27C").endCell();
        CellSlice cs2 = CellSlice.beginParse(c2);
        BigInteger j = cs2.loadUint(255);
        assertThat(j.longValue()).isEqualTo(73);
        cs2.endParse();
    }
}
