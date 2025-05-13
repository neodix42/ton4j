package org.ton.ton4j.tlb;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMap;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestTlbDeserialization {

  @Test
  public void testDeserializeVmStack() {
    String vmStackBase64 =
        "te6cckEBCgEAnAABCAAABAABAgIDAgMBEgEAAAAAAAAAAgQCAc0GBwESAf//////////BQAAAgEgCAkAQ0gBbF+J7on7AVGuEnLzWSSpDA1r2xEA7OCQ2nNAzO/RkZcAQyACpFRiTBVuUqppFmYbNfkOHAYYerTesChmOKus0fdUbnQAQyAB1Gq6XUDBGRE0RJeWUQfM5dt3qVy9IuK9RsVGGhBdOyyiu0ni";

    VmStack vmStack = VmStack.deserialize(CellSlice.beginParse(Cell.fromBocBase64(vmStackBase64)));

    VmStackValueTinyInt allowSeqNo =
        VmStackValueTinyInt.deserialize(
            CellSlice.beginParse(vmStack.getStack().getTos().get(0).toCell()));

    log.info("allow_arbitrary_order_seqno {}", allowSeqNo.getValue().longValue());

    VmStackValueTinyInt threshold =
        VmStackValueTinyInt.deserialize(
            CellSlice.beginParse(vmStack.getStack().getTos().get(1).toCell()));

    log.info("threshold {}", threshold.getValue().longValue());

    Cell signersCell =
        VmStackValueCell.deserialize(
                CellSlice.beginParse(vmStack.getStack().getTos().get(2).toCell()))
            .getCell();
    log.info("signersCell {}", signersCell.print());
    log.info("signersCellHex {}", signersCell.toHex());

    TonHashMap signers1 =
        CellSlice.beginParse(signersCell)
            .loadDict(
                8, k -> k.readUint(8), v -> MsgAddressInt.deserialize(CellSlice.beginParse(v)));
    // CellSlice.beginParse(v).loadRef()

    log.info("signers {}", signers1.toString());
  }

  @Test
  public void testDeserializeShardAccount() {
    String shardAccountBase64 =
        "te6cckECGwEABeUAAVA1YcjxZnm4yJe8h+8+aiyw0JXNXA03sHGSh5I7yqDxrAAAAAAAHoSCAQJvwAoPJFMpyH0WE1VvdbXSgFzUUdQdq9R2qa+GkzyHfqOYcjSKc4M+QKQYAAAAAAB6EhEF9KaP00ACAwEU/wD0pBP0vPLICwQBRQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgNgFgIBYgUGA8jQM9DTAwFxsJJfA+D6QDAi10nAAJhfA4ErDf4gMOAC0x8BIMAAmF8EgSsO/iAw4AHTPwHtRNDT/wEB0wcBAdTTBwEB9ATSAAEB0YErA/4gMCiCEPcYUQ+64w+BKxH+IDAFREQDBwgJAgEgEBEBqjiBKw/+IDAG0/8BKLOOEiCE/7qSMCSWUwW68uPw4gWkBd4B0gABAdMHAQHTLwEB1NEjkSaRKuJSMHj0Dm+h8uPvHscF8uPvIPgjvvLgbyD4I6FUbXAKApg2OCaCEHUJf126jroGghCjLFm/uo6p+CgYxwXy4GUD1NEQNBA2RlD4AH+OjSF49HxvpSCRMuMNAbPmWxA1UDSSNDbiUFQT4w0QNUAEDg0ANMhQBgHL/1AEAcsHEswBAcsH9AABAcoAye1UAeD4BwODDPlBMAODCPlBMPgHUAahgSf4AaBw+DaBEgZw+DaggSvscPg2oIEdmHD4NqAipgYioIEFOSagJ6Bw+DgjpIECmCegcPg4oAOmBliggQbgUAWgUAWgQwNw+DdZoAGgHL7y4GSBKxD+IDD4KFADCwL0AXACyFjPFgEBy//JiCLIywH0APQAywDJcCH5AHTIywISygfL/8nQBf4g/gCBKxH+IDCCEJxz+6L+IDAg/iAwyIIQnHP7olgKAssfyz8mAcsHUoDMUAsByy8bzCoBygAKm4ErFf4gMBkBywcIkTDicEATUImAGIBQ2zwVDACijk3IWAHLBVAFzxZQA/oCVHEjI+1B7UPtRO1F7UefW8hQA88XyRN3UAPLa8zM7WftZe1k7WPtYXR/7RGYdgHLa8wBzxftQe3xAfL/yQH7ANsGAuI2BNP/AQHTLwEB0wcBAdP/AQHU0fgoUAUBcALIWM8WAQHL/8mIIsjLAfQA9ADLAMlwAfkAdMjLAhLKB8v/ydAbxwXy4GUm+QAaulGTvhmw8uBmB/gjvvLgb0QUUFb4AH+OjSF49HxvpSCRMuMNAbPmWxUOAfoC10zQ0x8BIIIQ8TgeW7qOaoIQHQz707qOXmxE0wcBAdQhf3COF1ESePR8b6UyIZlTArry4GcCpALeAbMS5mwhIMIA8uBuI8IA8uBtUzC78uBtAfQEIX9wjhdREnj0fG+lMiGZUwK68uBnAqQC3gGzEuZsITDRVSORMOLjDQ8AEDDTB9QC+wDRAUO/dP9qJoaf+AgOmDgIDqaYOAgPoCaQAAgOiIGq+CgPwR0MEgIBZhMUAML4BwODDPlBMAODCPlBMPgHUAahgSf4AaBw+DaBEgZw+DaggSvscPg2oIEdmHD4NqAipgYioIEFOSagJ6Bw+DgjpIECmCegcPg4oAOmBliggQbgUAWgUAWgQwNw+DdZoAGgAPGwyvtRNDT/wEB0wcBAdTTBwEB9ATSAAEB0SN/cI4XURJ49HxvpTIhmVMCuvLgZwKkAt4BsxLmbCFSMLry4Gwhf3COF1ESePR8b6UyIZlTArry4GcCpALeAbMS5mwhMCLCAPLgbiTCAPLgbVJDu/LgbQGSM3+RA+IDgAVmwyf4KAEBcALIWM8WAQHL/8mIIsjLAfQA9ADLAMlwAfkAdMjLAhLKB8v/ydCAVCEIC0geU3zdLseC0KnatdMTesBli/s9YewBfRKTEIU29utoCAc0XGAIBIBkaAENIAWxfie6J+wFRrhJy81kkqQwNa9sRAOzgkNpzQMzv0ZGXAEMgAqRUYkwVblKqaRZmGzX5DhwGGHq03rAoZjirrNH3VG50AEMgAdRqul1AwRkRNESXllEHzOXbd6lcvSLivUbFRhoQXTssAO3PQg==";
    String shardAccountHex = Utils.base64ToHexString(shardAccountBase64);
    //    log.info(shardAccountBase64);
    Cell c1 = Cell.fromBoc(shardAccountHex);
    log.info(shardAccountHex);
    //    log.info(c1.print());
    ShardAccount shardAccount = ShardAccount.deserialize(CellSlice.beginParse(c1));
    log.info(shardAccount.toString());
    //    log.info(shardAccount.toCell().toBase64());
    log.info(shardAccount.toCell().toHex());
    Cell c2 = Cell.fromBoc(shardAccount.toCell().toHex());
    ShardAccount shardAccount2 =
        ShardAccount.deserialize(CellSlice.beginParse(Cell.fromBoc(shardAccount.toCell().toHex())));
    log.info(shardAccount2.toString());
    //    log.info(c2.print());
    assertThat(c1.print()).isEqualTo(c2.print());
  }

  @Test
  public void testHashMapEDeserialization1() {

    // online
    String t =
        "b5ee9c72010105010076000201cd01020201200304004348016c5f89ee89fb0151ae1272f35924a90c0d6bdb1100ece090da7340ccefd1919700432002a454624c156e52aa6916661b35f90e1c06187ab4deb0286638abacd1f7546e7400432001d46aba5d40c11911344497965107cce5db77a95cbd22e2bd46c5461a105d3b2c";
    //    String t =
    //
    // "b5ee9c7241010601007a00010203010201cd02030201200405004348016c5f89ee89fb0151ae1272f35924a90c0d6bdb1100ece090da7340ccefd1919700432002a454624c156e52aa6916661b35f90e1c06187ab4deb0286638abacd1f7546e7400432001d46aba5d40c11911344497965107cce5db77a95cbd22e2bd46c5461a105d3b2cdbca08e7";

    Cell cellWithDict = CellBuilder.beginCell().fromBoc(t).endCell();
    log.info("cell {}", cellWithDict.print());

    CellSlice cs = CellSlice.beginParse(cellWithDict);

    TonHashMap loadedDict =
        cs.loadDict(8, k -> k.readUint(8), v -> MsgAddressInt.deserialize(CellSlice.beginParse(v)));

    log.info("Deserialized hashmap from cell {}, count {}", loadedDict, loadedDict.elements.size());

    //
  }

  @Test
  public void testSaveAddressWithZeros() {
    MsgAddressIntStd addr1 =
        MsgAddressIntStd.of("0:000212646ce2585c73ad02a115cd6e5c8485dda6cccd62e1b05385ff121e5a7c");
    MsgAddressIntStd addr2 =
        MsgAddressIntStd.of("0QAAAhJkbOJYXHOtAqEVzW5chIXdpszNYuGwU4X_Eh5afLsu");
    log.info("addr1 {}", addr1);
    log.info("addr2 {}", addr2);

    log.info("addr1 toAddress {}", addr1.toAddress());
    log.info("addr2 toAddress {}", addr2.toAddress());

    assertThat(addr1.toString())
        .isEqualTo("0:000212646ce2585c73ad02a115cd6e5c8485dda6cccd62e1b05385ff121e5a7c");
    assertThat(addr2.toString())
        .isEqualTo("0:000212646ce2585c73ad02a115cd6e5c8485dda6cccd62e1b05385ff121e5a7c");
  }

  @Test
  public void testNormalizedHash() {
    Cell c =
        CellBuilder.beginCell()
            .fromBocBase64(
                "te6ccgEBAgEAqgAB4YgA2ZpktQsYby0n9cV5VWOFINBjScIU2HdondFsK3lDpEAFG8W4Jpf7AeOqfzL9vZ79mX3eM6UEBxZvN6+QmpYwXBq32QOBIrP4lF5ijGgQmZbC6KDeiiptxmTNwl5f59OAGU1NGLsixYlYAAAA2AAcAQBoYgBZQOG7qXmeA/2Tw1pLX2IkcQ5h5fxWzzcBskMJbVVRsKNaTpAAAAAAAAAAAAAAAAAAAA==")
            .endCell();

    Message message = Message.deserialize(CellSlice.beginParse(c));
    log.info(message.toString());

    String hash = Utils.bytesToHex(message.getNormalizedHash());
    log.info("hash {}", hash);
    assertThat(hash).isEqualTo("23ff6f150d573f64d5599a57813f991882b7b4d5ae0550ebd08ea658431e62f6");
  }
}
