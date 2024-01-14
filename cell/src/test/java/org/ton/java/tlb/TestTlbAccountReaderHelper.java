package org.ton.java.tlb;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.loader.Tlb;
import org.ton.java.tlb.types.Account;
import org.ton.java.tlb.types.AccountHelper;
import org.ton.java.tlb.types.AccountStateActive;
import org.ton.java.tlb.types.AccountStorage;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestTlbAccountReaderHelper {

    @Test
    public void testLoadAccountStateFromCell() {
        Cell c = CellBuilder.fromBoc("b5ee9c724101030100d700026fc00c419e2b8a3b6cd81acd3967dbbaf4442e1870e99eaf32278b7814a6ccaac5f802068148c314b1854000006735d812370d00764ce8d340010200deff0020dd2082014c97ba218201339cbab19f71b0ed44d0d31fd31f31d70bffe304e0a4f2608308d71820d31fd31fd31ff82313bbf263ed44d0d31fd31fd3ffd15132baf2a15144baf2a204f901541055f910f2a3f8009320d74a96d307d402fb00e8d101a4c8cb1fcb1fcbffc9ed5400500000000229a9a317d78e2ef9e6572eeaa3f206ae5c3dd4d00ddd2ffa771196dc0ab985fa84daf451c340d7fa");
        CellSlice cs = CellSlice.beginParse(c);
        Account account = (Account) Tlb.load(Account.class, cs);
        log.info("accountState {}", account);
        assertThat(((AccountStateActive) account.getAccountStorage().getAccountState()).getStateInit().getCode().toString()).isEqualTo("FF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54");
        assertThat(((AccountStateActive) account.getAccountStorage().getAccountState()).getStateInit().getData().toString()).isEqualTo("0000000229A9A317D78E2EF9E6572EEAA3F206AE5C3DD4D00DDD2FFA771196DC0AB985FA84DAF451");
        assertThat(account.getAccountStorage().getBalance().getCoins()).isEqualTo(BigInteger.valueOf(31011747));
        assertThat(account.getAddress().toAddress().toString(true, true, true, false)).isEqualTo("EQDEGeK4o7bNgazTln27r0RC4YcOmerzIni3gUpsyqxfgMWk");
        assertThat(account.getAccountStorage().getAccountStatus()).isEqualTo("ACTIVE");
        assertThat(account.getAccountStorage().getLastTransactionLt()).isEqualTo(BigInteger.valueOf(28370239000003L));
    }

    @Test
    public void testGetMethodHash() {
        assertThat(AccountHelper.methodNameHash("seqno")).isEqualTo(85143);
    }

    @Test
    public void testHasGetMethod() {
        Cell c = CellBuilder.fromBoc("b5ee9c724102140100021f000114ff00f4a413f4bcf2c80b0102016202030202cd04050201200e0f04e7d10638048adf000e8698180b8d848adf07d201800e98fe99ff6a2687d20699fea6a6a184108349e9ca829405d47141baf8280e8410854658056b84008646582a802e78b127d010a65b509e58fe59f80e78b64c0207d80701b28b9e382f970c892e000f18112e001718112e001f181181981e0024060708090201200a0b00603502d33f5313bbf2e1925313ba01fa00d43028103459f0068e1201a44343c85005cf1613cb3fccccccc9ed54925f05e200a6357003d4308e378040f4966fa5208e2906a4208100fabe93f2c18fde81019321a05325bbf2f402fa00d43022544b30f00623ba9302a402de04926c21e2b3e6303250444313c85005cf1613cb3fccccccc9ed54002c323401fa40304144c85005cf1613cb3fccccccc9ed54003c8e15d4d43010344130c85005cf1613cb3fccccccc9ed54e05f04840ff2f00201200c0d003d45af0047021f005778018c8cb0558cf165004fa0213cb6b12ccccc971fb008002d007232cffe0a33c5b25c083232c044fd003d0032c03260001b3e401d3232c084b281f2fff2742002012010110025bc82df6a2687d20699fea6a6a182de86a182c40043b8b5d31ed44d0fa40d33fd4d4d43010245f04d0d431d430d071c8cb0701cf16ccc980201201213002fb5dafda89a1f481a67fa9a9a860d883a1a61fa61ff480610002db4f47da89a1f481a67fa9a9a86028be09e008e003e00b01a500c6e");
        AccountHelper accountHelper = AccountHelper.builder()
                .isActive(false)
                .state(null)
                .data(null)
                .code(c)
                .lastTxLt(BigInteger.ZERO)
                .lastTxHash(null)
                .build();

        assertThat(accountHelper.hasGetMethod("get_nft_content")).isTrue();
        assertThat(accountHelper.hasGetMethod("get_lol_content")).isFalse();
    }

    @Test
    public void testLoadAccountStorageFrozen() {

        Cell c = CellBuilder.beginCell()
                .storeUint(123, 64)
                .storeCoins(BigInteger.valueOf(123))
                .storeBit(false)
                .storeBit(false)
                .storeBit(true)
                .endCell();

        CellSlice cs = CellSlice.beginParse(c);
        AccountStorage accountStorage = (AccountStorage) Tlb.load(AccountStorage.class, cs);

        assertThat(accountStorage.getAccountStatus()).isEqualTo("FROZEN");
    }

    @Test
    public void testLoadAccountStorageUnint() {

        Cell c = CellBuilder.beginCell()
                .storeUint(123, 64)
                .storeCoins(BigInteger.valueOf(123))
                .storeBit(false)
                .storeBit(false)
                .storeBit(false)
                .endCell();

        CellSlice cs = CellSlice.beginParse(c);
        AccountStorage accountStorage = (AccountStorage) cs.loadTlb(AccountStorage.class);

        assertThat(accountStorage.getAccountStatus()).isEqualTo("UNINIT");
    }
}