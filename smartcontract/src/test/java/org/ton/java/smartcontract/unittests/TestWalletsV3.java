package org.ton.java.smartcontract.unittests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.v3.WalletV3R1;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.tlb.types.CurrencyCollection;
import org.ton.java.tlb.types.InternalMessageInfo;
import org.ton.java.tlb.types.Message;
import org.ton.java.tlb.types.MsgAddressIntStd;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletsV3 {

  @Test
  public void testNewWalletV3R1() {
    // echo "F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4" | xxd -r -p - >
    // new-wallet.pk
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    Tonlib tonlib =
        Tonlib.builder()
            .testnet(true)
            .ignoreCache(false)
            //                .verbosityLevel(VerbosityLevel.DEBUG)
            .build();

    WalletV3R1 contract =
        WalletV3R1.builder().wc(0).keyPair(keyPair).walletId(42).tonlib(tonlib).build();

    String codeAsHex = contract.getStateInit().getCode().bitStringToHex();
    String dataAsHex = contract.getStateInit().getData().bitStringToHex();
    String rawAddress = contract.getAddress().toRaw();
    log.info("rawAddress {}", rawAddress);
    log.info("nonBounceable {}", contract.getAddress().toNonBounceable());
    assertThat(codeAsHex)
        .isEqualTo(
            "FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54");

    assertThat(dataAsHex)
        .isEqualTo(
            "000000000000002A82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");

    assertThat(rawAddress)
        .isEqualTo("0:c1eb3db41c4d12ab22538c1255c1cbd32f2e9645ff6b8a1c574a3f4191921dc1");
  }

  /** >fift -s new-wallet-v3.fif 0 42 */
  @Test
  public void testNewWalletV3R2() {
    // echo "F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4" | xxd -r -p - >
    // new-wallet.pk
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    Tonlib tonlib =
        Tonlib.builder()
            .testnet(true)
            .ignoreCache(false)
            //                .verbosityLevel(VerbosityLevel.DEBUG)
            .build();

    WalletV3R2 contract =
        WalletV3R2.builder().wc(0).keyPair(keyPair).walletId(42).tonlib(tonlib).build();

    String codeAsHex = contract.getStateInit().getCode().bitStringToHex();
    String dataAsHex = contract.getStateInit().getData().bitStringToHex();
    String rawAddress = contract.getAddress().toRaw();

    assertThat(codeAsHex)
        .isEqualTo(
            "FF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54");
    assertThat(dataAsHex)
        .isEqualTo(
            "000000000000002A82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
    assertThat(rawAddress)
        .isEqualTo("0:2d29bfa071c8c62fa3398b661a842e60f04cb8a915fb3e749ef7c6c41343e16c");

    Message msg = contract.prepareDeployMsg();
    // external message for serialization
    assertThat(msg.toCell().bitStringToHex())
        .isEqualTo(
            "88005A537F40E3918C5F467316CC35085CC1E09971522BF67CE93DEF8D882687C2D811874B9A113E0A2328885A19E53A31DA6ADA97D9D03506EAA33E03C541C2098F409E248E570BBD9DE806DCCF1E0727873DFF3A6C969E4824D3D77025D96B040D01200000055FFFFFFFE00000001_");
    // final boc
    assertThat(Utils.bytesToHex(msg.toCell().toBoc(true)).toUpperCase())
        .isEqualTo(
            "B5EE9C724102030100010F0002DF88005A537F40E3918C5F467316CC35085CC1E09971522BF67CE93DEF8D882687C2D811874B9A113E0A2328885A19E53A31DA6ADA97D9D03506EAA33E03C541C2098F409E248E570BBD9DE806DCCF1E0727873DFF3A6C969E4824D3D77025D96B040D01200000055FFFFFFFE000000010010200DEFF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED540050000000000000002A82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FBAD8E6092");

    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(contract.getSeqno())
            .destination(Address.of(TestFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.8))
            .build();

    msg = contract.prepareExternalMsg(config);

    ExtMessageInfo ext = tonlib.sendRawMessage(msg.toCell().toBase64());
    log.info("ext {}", ext);
  }

  /** >fift -s new-wallet-v3.fif -1 42 */
  @Test
  public void testNewWalletV3R2Master() {
    // echo "F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4" | xxd -r -p - >
    // new-wallet.pk
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    WalletV3R2 contract = WalletV3R2.builder().wc(-1).keyPair(keyPair).walletId(42).build();

    String codeAsHex = contract.getStateInit().getCode().bitStringToHex();
    String dataAsHex = contract.getStateInit().getData().bitStringToHex();
    String rawAddress = contract.getAddress().toRaw();

    assertThat(codeAsHex)
        .isEqualTo(
            "FF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54");
    assertThat(dataAsHex)
        .isEqualTo(
            "000000000000002A82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
    assertThat(rawAddress)
        .isEqualTo("-1:2d29bfa071c8c62fa3398b661a842e60f04cb8a915fb3e749ef7c6c41343e16c");

    Message msg = contract.prepareDeployMsg();
    // external message for serialization
    assertThat(msg.toCell().bitStringToHex())
        .isEqualTo(
            "89FE5A537F40E3918C5F467316CC35085CC1E09971522BF67CE93DEF8D882687C2D811874B9A113E0A2328885A19E53A31DA6ADA97D9D03506EAA33E03C541C2098F409E248E570BBD9DE806DCCF1E0727873DFF3A6C969E4824D3D77025D96B040D01200000055FFFFFFFE00000001_");
    // final boc
    assertThat(Utils.bytesToHex(msg.toCell().toBoc(true)).toUpperCase())
        .isEqualTo(
            "B5EE9C724102030100010F0002DF89FE5A537F40E3918C5F467316CC35085CC1E09971522BF67CE93DEF8D882687C2D811874B9A113E0A2328885A19E53A31DA6ADA97D9D03506EAA33E03C541C2098F409E248E570BBD9DE806DCCF1E0727873DFF3A6C969E4824D3D77025D96B040D01200000055FFFFFFFE000000010010200DEFF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED540050000000000000002A82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB7EB09F49");
  }

  /**
   * >fift -s wallet-v3.fif new-wallet
   * 0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d 42 1 1 -t 1000
   */
  @Test
  public void testCreateTransferMessageWalletV3R2WithBounce11() {

    Address addr = Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d");
    Cell order =
        Message.builder()
            .info(
                InternalMessageInfo.builder()
                    .bounce(true)
                    .srcAddr(null)
                    .dstAddr(
                        MsgAddressIntStd.builder()
                            .workchainId(addr.wc)
                            .address(addr.toBigInteger())
                            .build())
                    .value(CurrencyCollection.builder().coins(Utils.toNano(1)).build())
                    .build())
            //              .init()
            //              .body()
            .build()
            .toCell();
    // good 000000000000000000000000000
    // bad  400000000000000000000000000
    System.out.println("order " + order.print());
    assertThat(order.print().trim())
        .isEqualTo(
            "x{620012C72A4B1C534C0572E9E3B1C157E9FA79971A2416D7E1BA8F19AC2C4E46F006A1DCD6500000000000000000000000000000}");
  }

  @Test
  public void testCreateTransferMessageWalletV3R2WithBounce() {
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    log.info("pubKey: {}", Utils.bytesToHex(keyPair.getPublicKey()));
    log.info("secKey: {}", Utils.bytesToHex(keyPair.getSecretKey()));

    WalletV3R2 contract = WalletV3R2.builder().wc(0).keyPair(keyPair).walletId(42).build();

    log.info("rawAddress {}", contract.getAddress().toRaw());

    WalletV3Config config =
        WalletV3Config.builder()
            .destination(
                Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
            .walletId(42)
            .seqno(1L)
            .amount(Utils.toNano(1))
            .bounce(true)
            .validUntil(1753376922)
            .build();

    Message msg = contract.prepareExternalMsg(config);

    log.info("extmsg {}", msg.toCell().print());
    // external message for serialization
    assertThat(msg.toCell().bitStringToHex())
        .isEqualTo(
            "88005A537F40E3918C5F467316CC35085CC1E09971522BF67CE93DEF8D882687C2D805623D1FDA58DD3880E03FFC5DF6DC6456A79E16F68BC7CAA1E74CFE1C12D4DB291B812D01C801932189992F16D78BBCFC6DCFE34DD2A9A431D301381BCCCBE86000000153441344D0000000081C_");
    // external message in BoC format
    assertThat(Utils.bytesToHex(msg.toCell().toBoc(true)).toUpperCase())
        .isEqualTo(
            "B5EE9C724101020100A90001DF88005A537F40E3918C5F467316CC35085CC1E09971522BF67CE93DEF8D882687C2D805623D1FDA58DD3880E03FFC5DF6DC6456A79E16F68BC7CAA1E74CFE1C12D4DB291B812D01C801932189992F16D78BBCFC6DCFE34DD2A9A431D301381BCCCBE86000000153441344D0000000081C010068620012C72A4B1C534C0572E9E3B1C157E9FA79971A2416D7E1BA8F19AC2C4E46F006A1DCD6500000000000000000000000000000676B3B92");
  }

  /**
   * >fift -s wallet-v3.fif new-wallet
   * 0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d 42 1 1 -n -t 1000
   */
  @Test
  public void testCreateTransferMessageWalletV3R2NoBounce() {
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    WalletV3R2 contract = WalletV3R2.builder().walletId(42).wc(0).keyPair(keyPair).build();

    WalletV3Config config =
        WalletV3Config.builder()
            .destination(
                Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
            .walletId(42)
            .seqno(1L)
            .amount(Utils.toNano(1))
            .bounce(false)
            .validUntil(1000)
            .build();

    Message msg = contract.prepareExternalMsg(config);
    // external message for serialization
    assertThat(msg.toCell().bitStringToHex())
        .isEqualTo(
            "88005A537F40E3918C5F467316CC35085CC1E09971522BF67CE93DEF8D882687C2D802738F80EEA52936E75BBFA1525E8037940497F143C704F0EE6AE8AF656EA225CCBC41511C8DEB37BC17C33258323E6C90DF5F7A19161EAF92CEB2566171C608180000015000001F40000000081C_");
    // external message in BoC format
    assertThat(Utils.bytesToHex(msg.toCell().toBoc(true)).toUpperCase())
        .isEqualTo(
            "B5EE9C724101020100A90001DF88005A537F40E3918C5F467316CC35085CC1E09971522BF67CE93DEF8D882687C2D802738F80EEA52936E75BBFA1525E8037940497F143C704F0EE6AE8AF656EA225CCBC41511C8DEB37BC17C33258323E6C90DF5F7A19161EAF92CEB2566171C608180000015000001F40000000081C010068420012C72A4B1C534C0572E9E3B1C157E9FA79971A2416D7E1BA8F19AC2C4E46F006A1DCD650000000000000000000000000000020855187");
  }

  /**
   * >fift -s wallet-v3.fif new-wallet
   * 0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d 42 1 1 -t 1000 -C gift
   */
  @Test
  public void testCreateTransferMessageWalletV3R2WithBounceAndComment() {
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    WalletV3R2 contract = WalletV3R2.builder().walletId(42).wc(0).keyPair(keyPair).build();

    WalletV3Config config =
        WalletV3Config.builder()
            .destination(
                Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
            .walletId(42)
            .seqno(1L)
            .amount(Utils.toNano(1))
            .bounce(true)
            .validUntil(1000)
            .body(MsgUtils.createTextMessageBody("gift"))
            .build();

    Message msg = contract.prepareExternalMsg(config);
    // external message for serialization
    assertThat(msg.toCell().bitStringToHex())
        .isEqualTo(
            "88005A537F40E3918C5F467316CC35085CC1E09971522BF67CE93DEF8D882687C2D80565D22D9452A21AECE10EA776A94032D2C084346971AB06EAE7D464BAC1266D23BAD143B9C79E2D878BD8CD19450AE7550F6BE28D27B988A95D34330CBD1AB8300000015000001F40000000081C_");
    // external message in BoC format
    assertThat(Utils.bytesToHex(msg.toCell().toBoc(true)).toUpperCase())
        .isEqualTo(
            "B5EE9C724101020100B10001DF88005A537F40E3918C5F467316CC35085CC1E09971522BF67CE93DEF8D882687C2D80565D22D9452A21AECE10EA776A94032D2C084346971AB06EAE7D464BAC1266D23BAD143B9C79E2D878BD8CD19450AE7550F6BE28D27B988A95D34330CBD1AB8300000015000001F40000000081C010078620012C72A4B1C534C0572E9E3B1C157E9FA79971A2416D7E1BA8F19AC2C4E46F006A1DCD650000000000000000000000000000000000000676966741AA6F393");
  }
}
