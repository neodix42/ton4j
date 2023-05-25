package org.ton.java.tlb;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.loader.Tlb;
import org.ton.java.tlb.types.AccountState;
import org.ton.java.tlb.types.StateUpdate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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

    @Test
    public void testLoadAccountStateFromCell() {
        Cell c = CellBuilder.fromBoc("b5ee9c724101030100d700026fc00c419e2b8a3b6cd81acd3967dbbaf4442e1870e99eaf32278b7814a6ccaac5f802068148c314b1854000006735d812370d00764ce8d340010200deff0020dd2082014c97ba218201339cbab19f71b0ed44d0d31fd31f31d70bffe304e0a4f2608308d71820d31fd31fd31ff82313bbf263ed44d0d31fd31fd3ffd15132baf2a15144baf2a204f901541055f910f2a3f8009320d74a96d307d402fb00e8d101a4c8cb1fcb1fcbffc9ed5400500000000229a9a317d78e2ef9e6572eeaa3f206ae5c3dd4d00ddd2ffa771196dc0ab985fa84daf451c340d7fa");
        CellSlice cs = CellSlice.beginParse(c);
        AccountState accountState = (AccountState) Tlb.load(AccountState.class, cs);
        log.info("accountState {}", accountState);
        assertThat(accountState.getAccountStorage().getStateInit().getCode().toString()).isEqualTo("FF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54");
        assertThat(accountState.getAccountStorage().getStateInit().getData().toString()).isEqualTo("0000000229A9A317D78E2EF9E6572EEAA3F206AE5C3DD4D00DDD2FFA771196DC0AB985FA84DAF451");
    }
}
