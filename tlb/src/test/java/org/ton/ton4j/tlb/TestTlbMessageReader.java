package org.ton.ton4j.tlb;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestTlbMessageReader {

  @Test
  public void testCornerInternalMessage() {
    Cell c =
        CellBuilder.beginCell()
            .fromBoc(
                "b5ee9c724101020100860001b36800bf4c6bdca25797e55d700c1a5448e2af5d1ac16f9a9628719a4e1eb2b44d85e33fd104a366f6fb17799871f82e00e4f2eb8ae6aaf6d3e0b3fb346cd0208e23725e14094ba15d20071f12260000446ee17a9b0cc8c028d8c001004d8002b374733831aac3455708e8f1d2c7f129540b982d3a5de8325bf781083a8a3d2a04a7f943813277f3ea")
            .endCell();
    InternalMessageInfo internalMessageInfo =
        InternalMessageInfo.deserialize(CellSlice.beginParse(c));
    log.info("internalMessage {}", internalMessageInfo);
    assertThat(internalMessageInfo.getIHRDisabled()).isTrue();
    assertThat(internalMessageInfo.getBounce()).isTrue();
    assertThat(internalMessageInfo.getBounced()).isFalse();
    assertThat(internalMessageInfo.getSrcAddr().toAddress().toRaw())
        .isEqualTo("0:5fa635ee512bcbf2aeb8060d2a247157ae8d60b7cd4b1438cd270f595a26c2f1");
    assertThat(internalMessageInfo.getDstAddr().toAddress().toRaw())
        .isEqualTo("-1:44128d9bdbec5de661c7e0b80393cbae2b9aabdb4f82cfecd1b34082388dc978");
    assertThat(internalMessageInfo.getValue().getCoins())
        .isEqualTo(BigInteger.valueOf(9980893000L));
    assertThat(internalMessageInfo.getIHRFee()).isEqualTo(BigInteger.ZERO);
    assertThat(internalMessageInfo.getFwdFee()).isEqualTo(BigInteger.valueOf(9406739L));
    assertThat(internalMessageInfo.getCreatedAt()).isEqualTo(1684018284L);
    assertThat(internalMessageInfo.getCreatedLt()).isEqualTo(BigInteger.valueOf(37621510000006L));
    // run golang test and compare
  }

  @Test
  public void testCornerMessage() {
    Cell c =
        CellBuilder.beginCell()
            .fromBoc(
                "b5ee9c724101020100860001b36800bf4c6bdca25797e55d700c1a5448e2af5d1ac16f9a9628719a4e1eb2b44d85e33fd104a366f6fb17799871f82e00e4f2eb8ae6aaf6d3e0b3fb346cd0208e23725e14094ba15d20071f12260000446ee17a9b0cc8c028d8c001004d8002b374733831aac3455708e8f1d2c7f129540b982d3a5de8325bf781083a8a3d2a04a7f943813277f3ea")
            .endCell();
    Message message = Message.deserialize(CellSlice.beginParse(c));
    log.info("internalMessage {}", message);
    InternalMessageInfo internalMessageInfo = (InternalMessageInfo) message.getInfo();
    assertThat(internalMessageInfo.getIHRDisabled()).isTrue();
    assertThat(internalMessageInfo.getValue().getCoins())
        .isEqualTo(BigInteger.valueOf(9980893000L));
    assertThat(internalMessageInfo.getFwdFee()).isEqualTo(BigInteger.valueOf(9406739L));
    assertThat(internalMessageInfo.getCreatedAt()).isEqualTo(1684018284L);
    assertThat(internalMessageInfo.getCreatedLt()).isEqualTo(BigInteger.valueOf(37621510000006L));
    // run golang test and compare
  }

  @Test
  public void testExternalMessage1() {
    Cell c =
        CellBuilder.beginCell()
            .fromBoc(
                "B5EE9C724102030100010F0002DF88009F4CFD8AB69CB20864160E3A40E4F578643B5B5B409C51A0215DA579D95E49F6119529DEF4481C60CD81087FC7B058797AFDCEBCC1BE127EE2C4707C1E1C0F3D12F955EC3DE1C63E714876A931F6C6F13E6980284238AA9F94B0EC5859B37C4DE1E5353462FFFFFFFFE000000010010200DEFF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED5400500000000029A9A31782A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB2452EEC2")
            .endCell();
    ExternalMessageInInfo externalMessageInInfo =
        ExternalMessageInInfo.deserialize(CellSlice.beginParse(c));
    log.info("externalMessage {}", externalMessageInInfo);
    assertThat(externalMessageInInfo.getDstAddr().toString())
        .isEqualTo("0:4fa67ec55b4e5904320b071d20727abc321dadada04e28d010aed2bcecaf24fb");
  }

  @Test
  public void testInternalMessageLoadFromCell() {
    Address src = Address.of("EQAOp1zuKuX4zY6L9rEdSLam7J3gogIHhfRu_gH70u2MQnmd");
    InternalMessageInfo internalMessageInfo =
        InternalMessageInfo.builder()
            .iHRDisabled(false)
            .bounce(true)
            .bounced(false)
            .srcAddr(
                MsgAddressIntStd.builder().workchainId(src.wc).address(src.toBigInteger()).build())
            .dstAddr(
                MsgAddressIntStd.builder()
                    .workchainId((byte) 2)
                    .address(BigInteger.valueOf(2))
                    .build())
            .value(CurrencyCollection.builder().coins(Utils.toNano(0.5)).build())
            .createdAt(5L)
            .createdLt(BigInteger.valueOf(2))
            .build();

    InternalMessageInfo loadedInternalMessageInfo =
        InternalMessageInfo.deserialize(CellSlice.beginParse(internalMessageInfo.toCell()));
    log.info("loadedInternalMessage {}", loadedInternalMessageInfo);
    assertThat(loadedInternalMessageInfo.getValue().getCoins())
        .isEqualTo(BigInteger.valueOf(500000000L));
    assertThat(loadedInternalMessageInfo.getCreatedLt()).isEqualTo(BigInteger.valueOf(2L));
    assertThat(loadedInternalMessageInfo.getCreatedAt()).isEqualTo(5L);
  }

  @Test
  public void testMessageLoadFromCell() {
    InternalMessageInfo internalMessageInfo =
        InternalMessageInfo.builder()
            .iHRDisabled(false)
            .bounce(true)
            .bounced(false)
            .srcAddr(
                MsgAddressIntStd.builder()
                    .workchainId((byte) 2)
                    .address(BigInteger.valueOf(2))
                    .build())
            .dstAddr(
                MsgAddressIntStd.builder()
                    .workchainId((byte) 2)
                    .address(BigInteger.valueOf(2))
                    .build())
            .value(CurrencyCollection.builder().coins(Utils.toNano(0.5)).build())
            .createdAt(5L)
            .createdLt(BigInteger.valueOf(2))
            .build();

    InternalMessageInfo loadedMessage =
        InternalMessageInfo.deserialize(CellSlice.beginParse(internalMessageInfo.toCell()));
    log.info("loadedMessage {}", loadedMessage);
    assertThat(loadedMessage.getCreatedAt()).isEqualTo(5);
  }

  @Test
  public void testExternalMessageLoadFromCell() {
    ExternalMessageInInfo externalMessageInInfo =
        ExternalMessageInInfo.builder()
            .srcAddr(MsgAddressExtNone.builder().build())
            .dstAddr(
                MsgAddressIntStd.builder()
                    .workchainId((byte) 2)
                    .address(BigInteger.valueOf(2))
                    .build())
            .importFee(BigInteger.TEN)
            .build();

    ExternalMessageInInfo loadedMessage =
        ExternalMessageInInfo.deserialize(CellSlice.beginParse(externalMessageInInfo.toCell()));
    log.info("loadedMessage {}", loadedMessage);
    assertThat(loadedMessage.getImportFee()).isEqualTo(BigInteger.TEN);
  }

  @Test
  public void testExternalMessageOutLoadFromCell() {
    ExternalMessageOutInfo externalMessageOutInfo =
        ExternalMessageOutInfo.builder()
            .srcAddr(
                MsgAddressIntStd.builder()
                    .workchainId((byte) 2)
                    .address(BigInteger.valueOf(2))
                    .build())
            .dstAddr(MsgAddressExtNone.builder().build())
            .createdLt(BigInteger.TEN)
            .createdAt(5L)
            .build();

    ExternalMessageOutInfo loadedMessage =
        ExternalMessageOutInfo.deserialize(CellSlice.beginParse(externalMessageOutInfo.toCell()));
    log.info("loadedMessage {}", loadedMessage);
    assertThat(loadedMessage.getCreatedAt()).isEqualTo(5);
  }
}
