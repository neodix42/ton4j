package org.ton.java.tlb;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Slf4j
@RunWith(JUnit4.class)
public class TestTlbAccountReaderHelper {

  @Test
  public void testLoadAccountStateFromCell1() {
    Cell c =
        CellBuilder.beginCell()
            .fromBoc(
                "b5ee9c720102160100033c000271c006f5bc67986e06430961d9df00433926a4cd92e597ddd8aa6043645ac20bd178222c859043259e0d9000008f1590e4d10d405786bd75534001020114ff00f4a413f4bcf2c80b030051000000e929a9a317c1b3226ce226d6d818bafe82d3633aa0f06a6c677272d1f9b760ff0d0dcf56d8400201200405020148060704f8f28308d71820d31fd31fd31f02f823bbf264ed44d0d31fd31fd3fff404d15143baf2a15151baf2a205f901541064f910f2a3f80024a4c8cb1f5240cb1f5230cbff5210f400c9ed54f80f01d30721c0009f6c519320d74a96d307d402fb00e830e021c001e30021c002e30001c0039130e30d03a4c8cb1f12cb1fcbff1213141502e6d001d0d3032171b0925f04e022d749c120925f04e002d31f218210706c7567bd22821064737472bdb0925f05e003fa403020fa4401c8ca07cbffc9d0ed44d0810140d721f404305c810108f40a6fa131b3925f07e005d33fc8258210706c7567ba923830e30d03821064737472ba925f06e30d08090201200a0b007801fa00f40430f8276f2230500aa121bef2e0508210706c7567831eb17080185004cb0526cf1658fa0219f400cb6917cb1f5260cb3f20c98040fb0006008a5004810108f45930ed44d0810140d720c801cf16f400c9ed540172b08e23821064737472831eb17080185005cb055003cf1623fa0213cb6acb1fcb3fc98040fb00925f03e20201200c0d0059bd242b6f6a2684080a06b90fa0218470d4080847a4937d29910ce6903e9ff9837812801b7810148987159f31840201580e0f0011b8c97ed44d0d70b1f8003db29dfb513420405035c87d010c00b23281f2fff274006040423d029be84c6002012010110019adce76a26840206b90eb85ffc00019af1df6a26840106b90eb858fc0006ed207fa00d4d422f90005c8ca0715cbffc9d077748018c8cb05cb0222cf165005fa0214cb6b12ccccc973fb00c84014810108f451f2a7020070810108d718fa00d33fc8542047810108f451f2a782106e6f746570748018c8cb05cb025006cf165004fa0214cb6a12cb1fcb3fc973fb0002006c810108d718fa00d33f305224810108f459f2a782106473747270748018c8cb05cb025005cf165003fa0213cb6acb1f12cb3fc973fb00000af400c9ed54")
            .endCell();
    CellSlice cs = CellSlice.beginParse(c);
    org.ton.java.tlb.Account account = org.ton.java.tlb.Account.deserialize(cs);
    log.info("accountState {}", account);
  }

  @Test
  public void testLoadAccountStateFromCell2() {
    Cell c =
        CellBuilder.beginCell()
            .fromBoc(
                "b5ee9c724101030100d700026fc00c419e2b8a3b6cd81acd3967dbbaf4442e1870e99eaf32278b7814a6ccaac5f802068148c314b1854000006735d812370d00764ce8d340010200deff0020dd2082014c97ba218201339cbab19f71b0ed44d0d31fd31f31d70bffe304e0a4f2608308d71820d31fd31fd31ff82313bbf263ed44d0d31fd31fd3ffd15132baf2a15144baf2a204f901541055f910f2a3f8009320d74a96d307d402fb00e8d101a4c8cb1fcb1fcbffc9ed5400500000000229a9a317d78e2ef9e6572eeaa3f206ae5c3dd4d00ddd2ffa771196dc0ab985fa84daf451c340d7fa")
            .endCell();
    CellSlice cs = CellSlice.beginParse(c);
    Account account = Account.deserialize(cs);
    log.info("accountState {}", account);
    assertThat(
            ((AccountStateActive) account.getAccountStorage().getAccountState())
                .getStateInit()
                .getCode()
                .toString())
        .isEqualTo(
            "FF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54");
    assertThat(
            ((AccountStateActive) account.getAccountStorage().getAccountState())
                .getStateInit()
                .getData()
                .toString())
        .isEqualTo(
            "0000000229A9A317D78E2EF9E6572EEAA3F206AE5C3DD4D00DDD2FFA771196DC0AB985FA84DAF451");
    assertThat(account.getAccountStorage().getBalance().getCoins())
        .isEqualTo(BigInteger.valueOf(31011747));
    assertThat(account.getAddress().toAddress().toString(true, true, true, false))
        .isEqualTo("EQDEGeK4o7bNgazTln27r0RC4YcOmerzIni3gUpsyqxfgMWk");
    assertThat(account.getAccountStorage().getAccountStatus()).isEqualTo("ACTIVE");
    assertThat(account.getAccountStorage().getLastTransactionLt())
        .isEqualTo(BigInteger.valueOf(28370239000003L));
  }

  @Test
  public void testGetMethodHash() {
    assertThat(AccountHelper.methodNameHash("seqno")).isEqualTo(85143);
  }

  @Test
  public void testHasGetMethod() {
    Cell c =
        CellBuilder.beginCell()
            .fromBoc(
                "b5ee9c724102140100021f000114ff00f4a413f4bcf2c80b0102016202030202cd04050201200e0f04e7d10638048adf000e8698180b8d848adf07d201800e98fe99ff6a2687d20699fea6a6a184108349e9ca829405d47141baf8280e8410854658056b84008646582a802e78b127d010a65b509e58fe59f80e78b64c0207d80701b28b9e382f970c892e000f18112e001718112e001f181181981e0024060708090201200a0b00603502d33f5313bbf2e1925313ba01fa00d43028103459f0068e1201a44343c85005cf1613cb3fccccccc9ed54925f05e200a6357003d4308e378040f4966fa5208e2906a4208100fabe93f2c18fde81019321a05325bbf2f402fa00d43022544b30f00623ba9302a402de04926c21e2b3e6303250444313c85005cf1613cb3fccccccc9ed54002c323401fa40304144c85005cf1613cb3fccccccc9ed54003c8e15d4d43010344130c85005cf1613cb3fccccccc9ed54e05f04840ff2f00201200c0d003d45af0047021f005778018c8cb0558cf165004fa0213cb6b12ccccc971fb008002d007232cffe0a33c5b25c083232c044fd003d0032c03260001b3e401d3232c084b281f2fff2742002012010110025bc82df6a2687d20699fea6a6a182de86a182c40043b8b5d31ed44d0fa40d33fd4d4d43010245f04d0d431d430d071c8cb0701cf16ccc980201201213002fb5dafda89a1f481a67fa9a9a860d883a1a61fa61ff480610002db4f47da89a1f481a67fa9a9a86028be09e008e003e00b01a500c6e")
            .endCell();
    AccountHelper accountHelper =
        AccountHelper.builder()
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

    Cell c =
        CellBuilder.beginCell()
            .storeUint(123, 64)
            .storeCoins(BigInteger.valueOf(123))
            .storeBit(false)
            .storeBit(false)
            .storeBit(true)
            .endCell();

    CellSlice cs = CellSlice.beginParse(c);
    AccountStorage accountStorage = AccountStorage.deserialize(cs);

    assertThat(accountStorage.getAccountStatus()).isEqualTo("FROZEN");
  }

  @Test
  public void testLoadAccountStorageUnint() {

    Cell c =
        CellBuilder.beginCell()
            .storeUint(123, 64)
            .storeCoins(BigInteger.valueOf(123))
            .storeBit(false)
            .storeBit(false)
            .storeBit(false)
            .endCell();

    CellSlice cs = CellSlice.beginParse(c);
    AccountStorage accountStorage = AccountStorage.deserialize(cs);

    assertThat(accountStorage.getAccountStatus()).isEqualTo("UNINIT");
  }
}
