package org.ton.java.tlb;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.loader.Tlb;
import org.ton.java.tlb.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestTlbMessageReader {

    @Test
    public void testCornerMessage() {
        Cell c = CellBuilder.fromBoc("b5ee9c724101020100860001b36800bf4c6bdca25797e55d700c1a5448e2af5d1ac16f9a9628719a4e1eb2b44d85e33fd104a366f6fb17799871f82e00e4f2eb8ae6aaf6d3e0b3fb346cd0208e23725e14094ba15d20071f12260000446ee17a9b0cc8c028d8c001004d8002b374733831aac3455708e8f1d2c7f129540b982d3a5de8325bf781083a8a3d2a04a7f943813277f3ea");
        InternalMessage internalMessage = (InternalMessage) Tlb.load(InternalMessage.class, CellSlice.beginParse(c));
        log.info("internalMessage {}", internalMessage);
        log.info("internalMessage body {}", internalMessage.getBody().toHex());
        log.info("internalMessage body {}", internalMessage.getBody().print());
        assertThat(internalMessage.isIHRDisabled()).isTrue();
        assertThat(internalMessage.getValue().getCoins()).isEqualTo(BigInteger.valueOf(9980893000L));
        assertThat(internalMessage.getFwdFee()).isEqualTo(BigInteger.valueOf(9406739L));
        assertThat(internalMessage.getCreatedAt()).isEqualTo(1684018284L);
        assertThat(internalMessage.getCreatedLt()).isEqualTo(BigInteger.valueOf(37621510000006L));
        //run golang test and compare
    }

    @Test
    public void testExternalMessage1() {
        Cell c = CellBuilder.fromBoc("B5EE9C724102030100010F0002DF88009F4CFD8AB69CB20864160E3A40E4F578643B5B5B409C51A0215DA579D95E49F6119529DEF4481C60CD81087FC7B058797AFDCEBCC1BE127EE2C4707C1E1C0F3D12F955EC3DE1C63E714876A931F6C6F13E6980284238AA9F94B0EC5859B37C4DE1E5353462FFFFFFFFE000000010010200DEFF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED5400500000000029A9A31782A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB2452EEC2");
        ExternalMessage externalMessage = (ExternalMessage) Tlb.load(ExternalMessage.class, CellSlice.beginParse(c));
        log.info("externalMessage {}", externalMessage);

        assertThat(externalMessage.getStateInit().getCode().toString()).isEqualTo("FF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54");
        assertThat(externalMessage.getStateInit().getData().toString()).isEqualTo("0000000029A9A31782A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
        assertThat(externalMessage.getBody().toString()).isEqualTo("A94EF7A240E3066C0843FE3D82C3CBD7EE75E60DF093F7162383E0F0E079E897CAAF61EF0E31F38A43B5498FB63789F34C014211C554FCA58762C2CD9BE26F0F29A9A317FFFFFFFF00000000");
        assertThat(externalMessage.getDstAddr().getMsgAddressInt().toString()).isEqualTo("0:4fa67ec55b4e5904320b071d20727abc321dadada04e28d010aed2bcecaf24fb");
    }

    @Test
    public void testInternalMessageLoadFromCell() {
        InternalMessage internalMessage = InternalMessage.builder()
                .iHRDisabled(false)
                .bounce(true)
                .bounced(false)
                //    .srcAddr(Address.of("EQAOp1zuKuX4zY6L9rEdSLam7J3gogIHhfRu_gH70u2MQnmd"))
                //  .dstAddr(Address.of("EQA_B407fiLIlE5VYZCaI2rki0in6kLyjdhhwitvZNfpe7eY"))
                .value(CurrencyCollection.builder().coins(Utils.toNano(0.5)).build())
                .createdAt(5L)
                .createdLt(BigInteger.TWO)
                .stateInit(StateInit.builder()
                        .code(CellBuilder.beginCell().endCell())
                        .data(CellBuilder.beginCell().endCell())
                        .build())
                .body(CellBuilder.beginCell().endCell())
                .build();

        InternalMessage loadedInternalMessage = (InternalMessage) Tlb.load(InternalMessage.class, CellSlice.beginParse(internalMessage.toCell()));
        log.info("loadedInternalMessage {}", loadedInternalMessage);
        assertThat(loadedInternalMessage.getValue().getCoins()).isEqualTo(BigInteger.valueOf(500000000L));
        assertThat(loadedInternalMessage.getCreatedLt()).isEqualTo(BigInteger.valueOf(2L));
        assertThat(loadedInternalMessage.getCreatedAt()).isEqualTo(5L);
    }

    @Test
    public void testMessageLoadFromCell() {
        InternalMessage internalMessage = InternalMessage.builder()
                .iHRDisabled(false)
                .bounce(true)
                .bounced(false)
                .srcAddr(null)
                .dstAddr(null)
                .value(CurrencyCollection.builder().coins(Utils.toNano(0.5)).build())
                .createdAt(5L)
                .createdLt(BigInteger.TWO)
                .stateInit(null)
                .body(CellBuilder.beginCell().storeUint(369, 27).endCell())
                .build();

        Message loadedMessage = (Message) Tlb.load(Message.class, CellSlice.beginParse(internalMessage.toCell()));
        log.info("loadedMessage {}", loadedMessage);
        assertThat(loadedMessage.getMsgType()).isEqualTo("INTERNAL");
    }

    @Test
    public void testExternalMessageLoadFromCell() {
        ExternalMessage externalMessage = ExternalMessage.builder()
                .srcAddr(null)
                .dstAddr(null)
                .importFee(BigInteger.TEN)
                .stateInit(null)
                .body(CellBuilder.beginCell().storeUint(369, 27).endCell())
                .build();

        Message loadedMessage = (Message) Tlb.load(Message.class, CellSlice.beginParse(externalMessage.toCell()));
        log.info("loadedMessage {}", loadedMessage);
        assertThat(loadedMessage.getMsgType()).isEqualTo("EXTERNAL_IN");
    }

    @Test
    public void testExternalMessageOutLoadFromCell() {
        ExternalMessageOut externalMessageOut = ExternalMessageOut.builder()
                .srcAddr(null)
                .dstAddr(null)
                .createdLt(BigInteger.TEN)
                .createdAt(5L)
                .stateInit(null)
                .body(CellBuilder.beginCell().storeUint(369, 27).endCell())
                .build();

        Message loadedMessage = (Message) Tlb.load(Message.class, CellSlice.beginParse(externalMessageOut.toCell()));
        log.info("loadedMessage {}", loadedMessage);
        assertThat(loadedMessage.getMsgType()).isEqualTo("EXTERNAL_OUT");
    }

    //https://tonsandbox.com/explorer/address/EQBKNxSb8ZItjuVB0C-f_idtdAc0S389DZxFwaFZVegBFEn8/11830865000003_161a9cc5a7de2a03aeba4d9cdbbab747a18148ee9dccabdf981f93a353619391
    @Test
    public void testExternalMessage2() {
        String bocHex = Utils.base64ToHexString("te6cckEBAgEAgQABQ4AUfOW61YF/y1MIwE8E1RvkKBdIVHAgdBGTjidHc8Yc9XABALRiYWZ5YmVpY3Q1bzJua2lqbmRheG16enB5bjZjZm1jcnRiZmI3N2Y0bDNqemQzdGE0a2ViY2hsaGVsdS5pcGZzLm5mdHN0b3JhZ2UubGluay9kYXRhLmpzb27XOQ8v");
        log.info("bocHex: {}", bocHex);
        Cell c = CellBuilder.fromBoc(bocHex);
        ExternalMessage externalMessage = (ExternalMessage) Tlb.load(ExternalMessage.class, CellSlice.beginParse(c));
        log.info("externalMessage {}", externalMessage);
//
//        assertThat(externalMessage.getStateInit().getCode().toString()).isEqualTo("FF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54");
//        assertThat(externalMessage.getStateInit().getData().toString()).isEqualTo("0000000029A9A31782A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
//        assertThat(externalMessage.getBody().toString()).isEqualTo("A94EF7A240E3066C0843FE3D82C3CBD7EE75E60DF093F7162383E0F0E079E897CAAF61EF0E31F38A43B5498FB63789F34C014211C554FCA58762C2CD9BE26F0F29A9A317FFFFFFFF00000000");
//        assertThat(externalMessage.getDstAddr().toString(false)).isEqualTo("0:4fa67ec55b4e5904320b071d20727abc321dadada04e28d010aed2bcecaf24fb");
    }

    //https://tonsandbox.com/explorer/address/EQBKNxSb8ZItjuVB0C-f_idtdAc0S389DZxFwaFZVegBFEn8/11830865000003_161a9cc5a7de2a03aeba4d9cdbbab747a18148ee9dccabdf981f93a353619391
    @Test
    public void testInternalMessage2() {
        String bocHex = Utils.base64ToHexString("te6cckEBAwEAnAABLwAAAAHHmiv5MdZBawAAAAAAAAAfNMS0CAEBQ4AUfOW61YF/y1MIwE8E1RvkKBdIVHAgdBGTjidHc8Yc9XACALRiYWZ5YmVpY3Q1bzJua2lqbmRheG16enB5bjZjZm1jcnRiZmI3N2Y0bDNqemQzdGE0a2ViY2hsaGVsdS5pcGZzLm5mdHN0b3JhZ2UubGluay9kYXRhLmpzb27Kwi+Q");
        log.info("bocHex: {}", bocHex);
        Cell c = CellBuilder.fromBoc(bocHex);
        log.info("cell " + c.print());
        InternalMessage internalMessage = (InternalMessage) Tlb.load(InternalMessage.class, CellSlice.beginParse(c));
        log.info("internalMessage {}", internalMessage);
    }

}