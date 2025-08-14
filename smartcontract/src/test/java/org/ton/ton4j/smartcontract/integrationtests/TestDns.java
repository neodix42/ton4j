package org.ton.ton4j.smartcontract.integrationtests;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.ton.ton4j.smartcontract.dns.Dns.DNS_CATEGORY_NEXT_RESOLVER;
import static org.ton.ton4j.smartcontract.dns.Dns.DNS_CATEGORY_WALLET;

import java.math.BigInteger;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.GenerateWallet;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.dns.Dns;
import org.ton.ton4j.smartcontract.dns.DnsCollection;
import org.ton.ton4j.smartcontract.dns.DnsItem;
import org.ton.ton4j.smartcontract.dns.DnsUtils;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.AuctionInfo;
import org.ton.ton4j.smartcontract.types.CollectionData;
import org.ton.ton4j.smartcontract.types.ItemData;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R1;
import org.ton.ton4j.toncenter.Network;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestDns extends CommonTest {
  static WalletV3R1 adminWallet;
  static WalletV3R1 buyerWallet;

  @Test
  public void testDnsResolveTestnet() {
    Dns dns = Dns.builder().tonlib(tonlib).build();
    log.info(
        "root DNS address = {}",
        dns.getRootDnsAddress()); // Ef_v5x0Thgr6pq6ur2NvkWhIf4DxAxsL-Nk5rknT6n99oPKX

    Object result = dns.resolve("apple.ton", DNS_CATEGORY_NEXT_RESOLVER, true);
    String resolvedAddress = ((Address) result).toBounceable();
    log.info("apple.ton resolved to {}", resolvedAddress);
    assertThat(resolvedAddress).isNotEmpty();

    //  item EQD9YWaIR_M_FIDQbNP6S8miv-3FU7kIHMRqg_S6bH_bDowf
    //  owner EQAsEbAKNuRFDkoB6PjYP2dPTdHgt1rX2szkFFHahuDOEkbB
    //  new owner EQBCMRzsJBTMDqF5JW8Mbq9Ap7b88qKxkwktlZEChtLbiFIH
    Address addr =
        (Address) dns.resolve("alice-alice-alice-9.ton", DNS_CATEGORY_NEXT_RESOLVER, true);
    log.info("alice-alice-alice-9 resolved to {}", addr.toString(true, true, true));
  }

  @Test
  public void testDnsResolveTestnetAdnlLiteClient() throws Exception {
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .liteServerIndex(0)
            .build();
    Dns dns = Dns.builder().adnlLiteClient(adnlLiteClient).build();
    log.info("root DNS address = {}", dns.getRootDnsAddress());

    Object result = dns.resolve("apple.ton", DNS_CATEGORY_NEXT_RESOLVER, true);
    String resolvedAddress = ((Address) result).toBounceable();
    log.info("apple.ton resolved to {}", resolvedAddress);
    assertThat(resolvedAddress).isNotEmpty();

    //  item EQD9YWaIR_M_FIDQbNP6S8miv-3FU7kIHMRqg_S6bH_bDowf
    //  owner EQAsEbAKNuRFDkoB6PjYP2dPTdHgt1rX2szkFFHahuDOEkbB
    //  new owner EQBCMRzsJBTMDqF5JW8Mbq9Ap7b88qKxkwktlZEChtLbiFIH
    Address addr =
        (Address) dns.resolve("alice-alice-alice-9.ton", DNS_CATEGORY_NEXT_RESOLVER, true);
    log.info("alice-alice-alice-9 resolved to {}", addr.toString(true, true, true));
  }

  @Test
  public void testDnsResolveMainnet() {
    Dns dns = Dns.builder().tonlib(tonlib).build();
    Address rootAddress = dns.getRootDnsAddress();
    log.info("root DNS address = {}", rootAddress.toString(true, true, true));

    Object result = dns.resolve("apple.ton", DNS_CATEGORY_NEXT_RESOLVER, true);
    assertThat(result).isNotNull();
    log.info("apple.ton resolved to {}", ((Address) result).toString(true, true, true));

    Address addr = (Address) dns.getWalletAddress("foundation.ton");
    log.info("foundation.ton resolved to {}", addr.toString(true, true, true));
    assertThat(addr).isNotNull();
  }

  @Test
  public void testDnsResolveMainnetAdnlLiteClient() throws Exception {
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .liteServerIndex(0)
            .build();
    Dns dns = Dns.builder().adnlLiteClient(adnlLiteClient).build();
    Address rootAddress = dns.getRootDnsAddress();
    log.info("root DNS address = {}", rootAddress.toString(true, true, true));

    Object result = dns.resolve("apple.ton", DNS_CATEGORY_NEXT_RESOLVER, true);
    assertThat(result).isNotNull();
    log.info("apple.ton resolved to {}", ((Address) result).toString(true, true, true));

    Address addr = (Address) dns.getWalletAddress("foundation.ton");
    log.info("foundation.ton resolved to {}", addr.toString(true, true, true));
    assertThat(addr).isNotNull();
  }

  @Test
  public void testDnsResolveTestnetTonCenterClient() throws Exception {
    TonCenter tonCenterClient =
        TonCenter.builder().apiKey(TESTNET_API_KEY).network(Network.TESTNET).build();

    Dns dns = Dns.builder().tonCenterClient(tonCenterClient).build();
    log.info("root DNS address = {}", dns.getRootDnsAddress());

    Object result = dns.resolve("apple.ton", DNS_CATEGORY_NEXT_RESOLVER, true);
    String resolvedAddress = ((Address) result).toBounceable();
    log.info("apple.ton resolved to {}", resolvedAddress);
    assertThat(resolvedAddress).isNotEmpty();

    Address addr =
        (Address) dns.resolve("alice-alice-alice-9.ton", DNS_CATEGORY_NEXT_RESOLVER, true);
    log.info("alice-alice-alice-9 resolved to {}", addr.toString(true, true, true));
  }

  @Test
  public void testDnsResolveMainnetTonCenterClient() throws Exception {
    TonCenter tonCenterClient =
        TonCenter.builder().apiKey("mainnet-api-key").network(Network.MAINNET).build();

    Dns dns = Dns.builder().tonCenterClient(tonCenterClient).build();
    Address rootAddress = dns.getRootDnsAddress();
    log.info("root DNS address = {}", rootAddress.toString(true, true, true));

    Object result = dns.resolve("apple.ton", DNS_CATEGORY_NEXT_RESOLVER, true);
    assertThat(result).isNotNull();
    log.info("apple.ton resolved to {}", ((Address) result).toString(true, true, true));

    Address addr = (Address) dns.getWalletAddress("foundation.ton");
    log.info("foundation.ton resolved to {}", addr.toString(true, true, true));
    assertThat(addr).isNotNull();
  }

  @Test
  public void testDnsRootDeploy() throws InterruptedException {

    adminWallet = GenerateWallet.randomV3R1(tonlib, 1);

    DnsRoot dnsRootContract =
        DnsRoot.builder()
            .tonlib(tonlib)
            .wc(0)
            .keyPair(adminWallet.getKeyPair())
            .address1(Address.of("EQC3dNlesgVD8YbAazcauIrXBPfiVhMMr5YYk2in0Mtsz0Bz"))
            .address2(Address.of("EQCA14o1-VWhS2efqoh_9M1b_A9DtKTuoqfmkn83AbJzwnPi"))
            .address3(Address.of("kQAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi47nL"))
            .build();
    log.info("new root DNS address {}", dnsRootContract.getAddress());

    WalletV3Config adminWalletConfig =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(1)
            .destination(dnsRootContract.getAddress())
            .amount(Utils.toNano(0.12))
            .stateInit(dnsRootContract.getStateInit())
            .build();

    SendResponse sendResponse = adminWallet.send(adminWalletConfig);
    assertThat(sendResponse.getCode()).isZero();

    dnsRootContract.waitForDeployment(45);

    assertThat(dnsRootContract.isDeployed()).isTrue();
  }

  @Test
  public void testDnsCollectionItemDeploy() throws InterruptedException {

    adminWallet = GenerateWallet.randomV3R1(tonlib, 20);
    buyerWallet = GenerateWallet.randomV3R1(tonlib, 20);

    log.info("admin wallet address {}", adminWallet.getAddress());
    //        log.info("buyer wallet address {}", buyerWallet.getAddress().toString(true, true,
    // true));

    String dnsItemCodeHex =
        "B5EE9C7241022801000698000114FF00F4A413F4BCF2C80B0102016202030202CC04050201201E1F02012006070201481819020120080902015816170201200A0B000D470C8CB01C9D0801F73E09DBC400B434C0C05C6C2497C1383E903E900C7E800C5C75C87E800C7E800C3C0289ECE39397C15B088D148CB1C17CB865407E90350C1B5C3232C1FD00327E08E08418B9101A68608209E3402A4108308324CC200337A0404B20403C162A20032A41287E08C0683C00911DFC02440D7E08FC02F814D671C1462C200C00113E910C1C2EBCB8536003F88E34109B5F0BFA40307020F8256D8040708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00E029C70091709509D31F50AAE221F008F82321BC24C0008E9E343A3A3B8E1636363737375135C705F2E196102510241023F823F00BE30EE0310DD33F256EB31FB0926C21E30D0D0E0F00FE302680698064A98452B0BEF2E19782103B9ACA0052A0A15270BC993682103B9ACA0019A193390805E220C2008E328210557CEA20F82510396D71708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00923036E2803C23F823A1A120C2009313A0029130E24474F0091024F823F00B00D2343653CDA182103B9ACA005210A15270BC993682103B9ACA0016A1923005E220C2008E378210370FEC516D72295134544743708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB001CA10B9130E26D5477655477632EF00B0204C882105FCC3D145220BA8E9531373B5372C705F2E191109A104910384706401504E082101A0B9D515220BA8E195B32353537375135C705F2E19A03D4304015045033F823F00BE02182104EB1F0F9BAE3023B20821044BEAE41BAE302382782104ED14B65BA1310111200885B363638385147C705F2E19B04D3FF20D74AC20007D0D30701C000F2E19CF404300798D43040168307F417983050058307F45B30E270C8CB07F400C910354014F823F00B01FE30363A246EF2E19D8050F833D0F4043052408307F40E6FA1F2E19FD30721C00022C001B1F2E1A021C0008E9124109B1068517A10571046105C43144CDD9630103A395F07E201C0018E32708210370FEC51586D8100A0708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00915BE21301FE8E7A37F8235006A1810258BC066E16B0F2E19E23D0D749F823F0075290BEF2E1975178A182103B9ACA00A120C2008E32102782104ED14B6558076D72708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB0093303535E2F82381012CA0F0024477F0091045103412F823F00BE05F041501F03502FA4021F001FA40D20031FA0082103B9ACA001DA121945314A0A1DE22D70B01C300209205A19135E220C2FFF2E192218E3E821005138D91C8500BCF16500DCF1671244B145448C0708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00106994102C395BE20114008A8E3528F0018210D53276DB103946096D71708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB0093383430E21045103412F823F00B009A32353582102FCB26A2BA8E3A7082108B77173504C8CBFF5005CF161443308040708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00E05F04840FF2F00093083001258C2040FA201938083001658C20407D200CB8083001A58C204064200A38083001E58C20404B2007B8083002258C204032200538083002650C20191EB83002A4E00C9D781E9C600069006AC0BC018060840EE6B2802A0060840EE6B2802A00A08418B9101A68608209E3402A410830856456F81B04A5A9D6A0192A4139200201201A1B0201201C1D0021081BA50C1B5C0838343E903E8034CFCC200017321400F3C5807E80B2CFF26000513B513434FFFE900835D2708027DFC07E9035353D0134CFCC0415C415B80C1C1B5B5B5B490415C415A0002B01B232FFD40173C59400F3C5B3333D0032CFF27B5520020120202102012024250013BBB39F00A175F07F008802027422230010A874F00A10475F07000CA959F00A6C71000DB8FCFF00A5F03802012026270013B64A5E014204EBE0FA1000C7B461843AE9240F152118001E5C08DE014206EBE0FA1A60E038001E5C339E8086007AE140F8001E5C33B84111C466105E033E04883DCB11FB64DDC4964AD1BA06B879240DC23572F37CC5CAAAB143A2FFFBC4180012660F003C003060FE81EDF4260F00306EB1583C";
    String dnsCollectionCodeHex =
        "B5EE9C7241021D010002C7000114FF00F4A413F4BCF2C80B0102016202030202CC040502012017180201200607020120131402012008090201200D0E016D420C70094840FF2F0DE01D0D3030171B0925F03E0FA403001D31FED44D0D4D4303122C000E30210245F048210370FEC51BADC840FF2F080A0201200B0C00D032F82320821062E44069BCF2E0C701F00420D74920C218F2E0C8208103F0BBF2E0C92078A908C000F2E0CA21F005F2E0CB58F00714BEF2E0CC22F9018050F833206EB38E10D0F4043052108307F40E6FA131F2D0CD9130E2C85004CF16C9C85003CF1612CCC9F00C000D1C3232C072742000331C27C074C1C07000082CE500A98200B784B98C4830003CB432600201200F100201201112004F3223880875D244B5C61673C58875D2883000082CE6C070007CB83280B50C3400A44C78B98C727420007F1C0875D2638D572E882CE38B8C00B4C1C8700B48F0802C0929BE14902E6C08B08BC8F04EAC2C48B09800F05EC4EC04AC6CC82CE500A98200B784F7B99B04AEA00093083001258C2040FA201938083001658C20407D200CB8083001A58C204064200A38083001E58C20404B2007B8083002258C204032200538083002650C20191EB83002A4E00C9D781E9C600069006AC0BC018060840EE6B2802A0060840EE6B2802A00A08418B9101A68608209E3402A410830856456F81B04A5A9D6A0192A41392002015815160039D2CF8053810F805BBC00C646582AC678B387D0165B5E66664C0207D804002D007232FFFE0A33C5B25C083232C044FD003D0032C03260001B3E401D3232C084B281F2FFF27420020120191A0201201B1C0007B8B5D318001FBA7A3ED44D0D4D43031F00A7001F00B8001BB905BED44D0D4D430307FF002128009DBA30C3020D74978A908C000F2E04620D70A07C00021D749C0085210B0935B786DE0209501D3073101DE21F0035122D71830F9018200BA93C8CB0F01820167A3ED43D8CF16C90191789170E212A0018F83DF327";

    DnsCollection dnsCollection =
        DnsCollection.builder()
            .tonlib(tonlib)
            .collectionContent(
                NftUtils.createOffChainUriCell(
                    UUID.randomUUID()
                        .toString()) // unique collection's http address each time for testing)
                )
            .dnsItemCodeHex(dnsItemCodeHex)
            .code(CellBuilder.beginCell().fromBoc(dnsCollectionCodeHex).endCell())
            .build();
    log.info("DNS collection address {}", dnsCollection.getAddress());

    WalletV3Config adminWalletConfig =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(1)
            .destination(dnsCollection.getAddress())
            .amount(Utils.toNano(1))
            .body(
                CellBuilder.beginCell()
                    .storeUint(0x370fec51, 32) // OP deploy new nft
                    .storeRef(
                        CellBuilder.beginCell().storeString("deploy nft collection").endCell())
                    .endCell())
            .stateInit(dnsCollection.getStateInit())
            .build();

    // deploy
    SendResponse sendResponse = adminWallet.send(adminWalletConfig);
    assertThat(sendResponse.getCode()).isZero();
    dnsCollection.waitForDeployment(60);

    getDnsCollectionInfo(tonlib, dnsCollection.getAddress());

    String dnsItem1DomainName =
        "alice-alice-alice-9"; // do not add .ton, it will fail with error code 203

    // create and deploy DNS Item
    adminWalletConfig =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .source(adminWallet.getAddress())
            .destination(dnsCollection.getAddress())
            .amount(Utils.toNano(11)) // mind min auction price, which is 10 tons
            .body(
                CellBuilder.beginCell()
                    .storeUint(0, 32) // OP deploy new nft
                    .storeRef(CellBuilder.beginCell().storeString(dnsItem1DomainName).endCell())
                    .endCell())
            .build();
    sendResponse = adminWallet.send(adminWalletConfig);

    assertThat(sendResponse.getCode()).isZero();

    adminWallet.waitForBalanceChange(45);

    Address dnsItem1Address =
        DnsCollection.getNftItemAddressByDomain(
            tonlib, dnsCollection.getAddress(), dnsItem1DomainName);
    log.info("dnsItem1 address {}", dnsItem1Address);

    tonlib.waitForDeployment(dnsItem1Address, 60);
    //        ContractUtils.waitForDeployment(tonlib, dnsItem1Address, 60);

    getDnsItemInfo(tonlib, dnsItem1Address);

    // make a bid
    WalletV3Config buyerConfig =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(buyerWallet.getSeqno())
            .destination(dnsItem1Address)
            .amount(Utils.toNano(13))
            .build();

    sendResponse = buyerWallet.send(buyerConfig);
    assertThat(sendResponse.getCode()).isZero();
    buyerWallet.waitForBalanceChange(45);

    Address dnsItem1Editor = DnsItem.getEditor(tonlib, dnsItem1Address);
    log.info("dnsItem1 editor {}", nonNull(dnsItem1Editor) ? dnsItem1Editor : null);

    Utils.sleep(60 * 4, "wait till auction is finished");

    getDnsItemInfo(tonlib, dnsItem1Address);

    // claim your domain by doing any action with it
    sendResponse = getStaticData(adminWallet, dnsItem1Address);
    assertThat(sendResponse.getCode()).isZero();
    Utils.sleep(30, "Claim DNS item " + dnsItem1DomainName);

    // or assign your wallet to it, so it could resolve your wallet address to your-domain.ton
    sendResponse = changeDnsRecord(buyerWallet, dnsItem1Address, buyerWallet.getAddress());
    assertThat(sendResponse.getCode()).isZero();
    Utils.sleep(30, "Assign wallet to domain " + dnsItem1DomainName);

    getDnsItemInfo(tonlib, dnsItem1Address);
    dnsItem1Editor = DnsItem.getEditor(tonlib, dnsItem1Address);
    log.info(
        "dnsItem1 editor {}",
        nonNull(dnsItem1Editor) ? dnsItem1Editor.toString(true, true, true) : null);

    sendResponse = governDnsItem(buyerWallet, dnsItem1Address);
    assertThat(sendResponse.getCode()).isZero();
    Utils.sleep(30, "govern dns item");

    sendResponse =
        transferDnsItem(
            buyerWallet, dnsItem1Address, "EQBCMRzsJBTMDqF5JW8Mbq9Ap7b88qKxkwktlZEChtLbiFIH");
    assertThat(sendResponse.getCode()).isZero();
    Utils.sleep(30, "transferring domain to other user");

    // release of domain is possible if either one year has passed and auction has ended;
    // also msg_value >= min_price and obviously only the owner can release domain name;
    // once it is released the owner changed to null and auction starts again
    sendResponse =
        releaseDnsItem(
            buyerWallet, dnsItem1Address, Utils.toNano(15)); // will fail with error code 414
    assertThat(sendResponse.getCode()).isZero();
    Utils.sleep(30);

    getDnsItemInfo(tonlib, dnsItem1Address);
  }

  @Test
  public void testDnsCollectionItemDeployTonCenter() throws Exception {
    TonCenter tonCenter =
            TonCenter.builder()
                    .apiKey(TESTNET_API_KEY)
                    .testnet()
                    .build();
    adminWallet = GenerateWallet.randomV3R1(tonCenter, 20);
    buyerWallet = GenerateWallet.randomV3R1(tonCenter, 20);

    log.info("admin wallet address {}", adminWallet.getAddress());

    String dnsItemCodeHex =
            "B5EE9C7241022801000698000114FF00F4A413F4BCF2C80B0102016202030202CC04050201201E1F02012006070201481819020120080902015816170201200A0B000D470C8CB01C9D0801F73E09DBC400B434C0C05C6C2497C1383E903E900C7E800C5C75C87E800C7E800C3C0289ECE39397C15B088D148CB1C17CB865407E90350C1B5C3232C1FD00327E08E08418B9101A68608209E3402A4108308324CC200337A0404B20403C162A20032A41287E08C0683C00911DFC02440D7E08FC02F814D671C1462C200C00113E910C1C2EBCB8536003F88E34109B5F0BFA40307020F8256D8040708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00E029C70091709509D31F50AAE221F008F82321BC24C0008E9E343A3A3B8E1636363737375135C705F2E196102510241023F823F00BE30EE0310DD33F256EB31FB0926C21E30D0D0E0F00FE302680698064A98452B0BEF2E19782103B9ACA0052A0A15270BC993682103B9ACA0019A193390805E220C2008E328210557CEA20F82510396D71708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00923036E2803C23F823A1A120C2009313A0029130E24474F0091024F823F00B00D2343653CDA182103B9ACA005210A15270BC993682103B9ACA0016A1923005E220C2008E378210370FEC516D72295134544743708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB001CA10B9130E26D5477655477632EF00B0204C882105FCC3D145220BA8E9531373B5372C705F2E191109A104910384706401504E082101A0B9D515220BA8E195B32353537375135C705F2E19A03D4304015045033F823F00BE02182104EB1F0F9BAE3023B20821044BEAE41BAE302382782104ED14B65BA1310111200885B363638385147C705F2E19B04D3FF20D74AC20007D0D30701C000F2E19CF404300798D43040168307F417983050058307F45B30E270C8CB07F400C910354014F823F00B01FE30363A246EF2E19D8050F833D0F4043052408307F40E6FA1F2E19FD30721C00022C001B1F2E1A021C0008E9124109B1068517A10571046105C43144CDD9630103A395F07E201C0018E32708210370FEC51586D8100A0708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00915BE21301FE8E7A37F8235006A1810258BC066E16B0F2E19E23D0D749F823F0075290BEF2E1975178A182103B9ACA00A120C2008E32102782104ED14B6558076D72708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB0093303535E2F82381012CA0F0024477F0091045103412F823F00BE05F041501F03502FA4021F001FA40D20031FA0082103B9ACA001DA121945314A0A1DE22D70B01C300209205A19135E220C2FFF2E192218E3E821005138D91C8500BCF16500DCF1671244B145448C0708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00106994102C395BE20114008A8E3528F0018210D53276DB103946096D71708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB0093383430E21045103412F823F00B009A32353582102FCB26A2BA8E3A7082108B77173504C8CBFF5005CF161443308040708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00E05F04840FF2F00093083001258C2040FA201938083001658C20407D200CB8083001A58C204064200A38083001E58C20404B2007B8083002258C204032200538083002650C20191EB83002A4E00C9D781E9C600069006AC0BC018060840EE6B2802A0060840EE6B2802A00A08418B9101A68608209E3402A410830856456F81B04A5A9D6A0192A4139200201201A1B0201201C1D0021081BA50C1B5C0838343E903E8034CFCC200017321400F3C5807E80B2CFF26000513B513434FFFE900835D2708027DFC07E9035353D0134CFCC0415C415B80C1C1B5B5B5B490415C415A0002B01B232FFD40173C59400F3C5B3333D0032CFF27B5520020120202102012024250013BBB39F00A175F07F008802027422230010A874F00A10475F07000CA959F00A6C71000DB8FCFF00A5F03802012026270013B64A5E014204EBE0FA1000C7B461843AE9240F152118001E5C08DE014206EBE0FA1A60E038001E5C339E8086007AE140F8001E5C33B84111C466105E033E04883DCB11FB64DDC4964AD1BA06B879240DC23572F37CC5CAAAB143A2FFFBC4180012660F003C003060FE81EDF4260F00306EB1583C";
    String dnsCollectionCodeHex =
            "B5EE9C7241021D010002C7000114FF00F4A413F4BCF2C80B0102016202030202CC040502012017180201200607020120131402012008090201200D0E016D420C70094840FF2F0DE01D0D3030171B0925F03E0FA403001D31FED44D0D4D4303122C000E30210245F048210370FEC51BADC840FF2F080A0201200B0C00D032F82320821062E44069BCF2E0C701F00420D74920C218F2E0C8208103F0BBF2E0C92078A908C000F2E0CA21F005F2E0CB58F00714BEF2E0CC22F9018050F833206EB38E10D0F4043052108307F40E6FA131F2D0CD9130E2C85004CF16C9C85003CF1612CCC9F00C000D1C3232C072742000331C27C074C1C07000082CE500A98200B784B98C4830003CB432600201200F100201201112004F3223880875D244B5C61673C58875D2883000082CE6C070007CB83280B50C3400A44C78B98C727420007F1C0875D2638D572E882CE38B8C00B4C1C8700B48F0802C0929BE14902E6C08B08BC8F04EAC2C48B09800F05EC4EC04AC6CC82CE500A98200B784F7B99B04AEA00093083001258C2040FA201938083001658C20407D200CB8083001A58C204064200A38083001E58C20404B2007B8083002258C204032200538083002650C20191EB83002A4E00C9D781E9C600069006AC0BC018060840EE6B2802A0060840EE6B2802A00A08418B9101A68608209E3402A410830856456F81B04A5A9D6A0192A41392002015815160039D2CF8053810F805BBC00C646582AC678B387D0165B5E66664C0207D804002D007232FFFE0A33C5B25C083232C044FD003D0032C03260001B3E401D3232C084B281F2FFF27420020120191A0201201B1C0007B8B5D318001FBA7A3ED44D0D4D43031F00A7001F00B8001BB905BED44D0D4D430307FF002128009DBA30C3020D74978A908C000F2E04620D70A07C00021D749C0085210B0935B786DE0209501D3073101DE21F0035122D71830F9018200BA93C8CB0F01820167A3ED43D8CF16C90191789170E212A0018F83DF327";

    DnsCollection dnsCollection =
            DnsCollection.builder()
                    .tonCenterClient(tonCenter)
                    .collectionContent(
                            NftUtils.createOffChainUriCell(
                                    UUID.randomUUID()
                                            .toString()) // unique collection's http address each time for testing)
                    )
                    .dnsItemCodeHex(dnsItemCodeHex)
                    .code(CellBuilder.beginCell().fromBoc(dnsCollectionCodeHex).endCell())
                    .build();
    log.info("DNS collection address {}", dnsCollection.getAddress());

    WalletV3Config adminWalletConfig =
            WalletV3Config.builder()
                    .walletId(42)
                    .seqno(1)
                    .destination(dnsCollection.getAddress())
                    .amount(Utils.toNano(1))
                    .body(
                            CellBuilder.beginCell()
                                    .storeUint(0x370fec51, 32) // OP deploy new nft
                                    .storeRef(
                                            CellBuilder.beginCell().storeString("deploy nft collection").endCell())
                                    .endCell())
                    .stateInit(dnsCollection.getStateInit())
                    .build();

    // deploy
    SendResponse sendResponse = adminWallet.send(adminWalletConfig);
    assertThat(sendResponse.getCode()).isZero();
    dnsCollection.waitForDeployment(60);

    getDnsCollectionInfo(tonCenter, dnsCollection.getAddress());

    String dnsItem1DomainName =
            "alice-alice-alice-9"; // do not add .ton, it will fail with error code 203

    // create and deploy DNS Item
    adminWalletConfig =
            WalletV3Config.builder()
                    .walletId(42)
                    .seqno(adminWallet.getSeqno())
                    .source(adminWallet.getAddress())
                    .destination(dnsCollection.getAddress())
                    .amount(Utils.toNano(11)) // mind min auction price, which is 10 tons
                    .body(
                            CellBuilder.beginCell()
                                    .storeUint(0, 32) // OP deploy new nft
                                    .storeRef(CellBuilder.beginCell().storeString(dnsItem1DomainName).endCell())
                                    .endCell())
                    .build();
    sendResponse = adminWallet.send(adminWalletConfig);
    assertThat(sendResponse.getCode()).isZero();

    adminWallet.waitForBalanceChange(45);

    Address dnsItem1Address =
            DnsCollection.getNftItemAddressByDomain(
                    tonCenter, dnsCollection.getAddress(), dnsItem1DomainName);
    log.info("dnsItem1 address {}", dnsItem1Address);

    tonCenter.waitForDeployment(dnsItem1Address, 60);
    //        ContractUtils.waitForDeployment(tonlib, dnsItem1Address, 60);

    getDnsItemInfo(tonCenter, dnsItem1Address);

    // make a bid
    WalletV3Config buyerConfig =
            WalletV3Config.builder()
                    .walletId(42)
                    .seqno(buyerWallet.getSeqno())
                    .destination(dnsItem1Address)
                    .amount(Utils.toNano(13))
                    .build();

    sendResponse = buyerWallet.send(buyerConfig);
    assertThat(sendResponse.getCode()).isZero();
    buyerWallet.waitForBalanceChange(45);

    Address dnsItem1Editor = DnsItem.getEditor(tonCenter, dnsItem1Address);
    log.info("dnsItem1 editor {}", nonNull(dnsItem1Editor) ? dnsItem1Editor : null);

    Utils.sleep(60 * 4, "wait till auction is finished");

    getDnsItemInfo(tonCenter, dnsItem1Address);

    // claim your domain by doing any action with it
    sendResponse = getStaticData(adminWallet, dnsItem1Address);
    assertThat(sendResponse.getCode()).isZero();
    Utils.sleep(30, "Claim DNS item " + dnsItem1DomainName);

    // or assign your wallet to it, so it could resolve your wallet address to your-domain.ton
    sendResponse = changeDnsRecord(buyerWallet, dnsItem1Address, buyerWallet.getAddress());
    assertThat(sendResponse.getCode()).isZero();
    Utils.sleep(30, "Assign wallet to domain " + dnsItem1DomainName);

    getDnsItemInfo(tonCenter, dnsItem1Address);
    dnsItem1Editor = DnsItem.getEditor(tonCenter, dnsItem1Address);
    log.info(
            "dnsItem1 editor {}",
            nonNull(dnsItem1Editor) ? dnsItem1Editor.toString(true, true, true) : null);

    sendResponse = governDnsItem(buyerWallet, dnsItem1Address);
    assertThat(sendResponse.getCode()).isZero();
    Utils.sleep(30, "govern dns item");

    sendResponse =
            transferDnsItem(
                    buyerWallet, dnsItem1Address, "EQBCMRzsJBTMDqF5JW8Mbq9Ap7b88qKxkwktlZEChtLbiFIH");
    assertThat(sendResponse.getCode()).isZero();
    Utils.sleep(30, "transferring domain to other user");

    // release of domain is possible if either one year has passed and auction has ended;
    // also msg_value >= min_price and obviously only the owner can release domain name;
    // once it is released the owner changed to null and auction starts again
    sendResponse =
            releaseDnsItem(
                    buyerWallet, dnsItem1Address, Utils.toNano(15)); // will fail with error code 414
    assertThat(sendResponse.getCode()).isZero();
    Utils.sleep(30);

    getDnsItemInfo(tonCenter, dnsItem1Address);
  }

  @Test
  public void testDnsItemDeployAtGlobalCollection() throws InterruptedException {

    adminWallet = GenerateWallet.randomV3R1(tonlib, 5);
    log.info("admin wallet address {}", adminWallet.getAddress());

    Address dnsCollectionAddress = Address.of("EQDjPtM6QusgMgWfl9kMcG-EALslbTITnKcH8VZK1pnH3UZA");
    log.info("DNS collection address {}", dnsCollectionAddress.toBounceable());

    getDnsCollectionInfo(tonlib, dnsCollectionAddress);

    resolveDnsCollectionItems(dnsCollectionAddress);

    String dnsItem1DomainName = "alice-alice-alice-12";

    // create and deploy DNS Item
    WalletV3Config adminWalletConfig =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(dnsCollectionAddress)
            .amount(Utils.toNano(15))
            .body(
                CellBuilder.beginCell()
                    .storeUint(0, 32) // OP deploy new nft
                    .storeRef(CellBuilder.beginCell().storeString(dnsItem1DomainName).endCell())
                    .endCell())
            .build();
    SendResponse sendResponse = adminWallet.send(adminWalletConfig);
    assertThat(sendResponse.getCode()).isZero();
    Utils.sleep(30, "deploying DNS item " + dnsItem1DomainName);

    Address dnsItem1Address =
        DnsCollection.getNftItemAddressByDomain(tonlib, dnsCollectionAddress, dnsItem1DomainName);
    log.info("dnsItem1 address {}", dnsItem1Address.toString(true, true, true));

    getDnsItemInfo(tonlib, dnsItem1Address);

    Utils.sleep(60 * 4, "wait till auction is finished");

    getDnsItemInfo(tonlib, dnsItem1Address);

    // claim your domain by doing any action with it
    sendResponse = getStaticData(adminWallet, dnsItem1Address);
    assertThat(sendResponse.getCode()).isZero();

    // assign your wallet to domain name
    sendResponse = changeDnsRecord(adminWallet, dnsItem1Address, adminWallet.getAddress());
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(25);

    getDnsItemInfo(tonlib, dnsItem1Address);

    Dns dns = Dns.builder().tonlib(tonlib).build();
    Address dnsRootAddress = dns.getRootDnsAddress();
    log.info("root DNS address = {}", dnsRootAddress.toString(true, true, true));

    Address addrDomain =
        (Address) dns.resolve(dnsItem1DomainName + ".ton", DNS_CATEGORY_NEXT_RESOLVER, true);
    log.info("{} resolved to {}", dnsItem1DomainName + ".ton", addrDomain);

    Address addrWallet = (Address) dns.getWalletAddress(dnsItem1DomainName + ".ton");
    log.info("{} resolved to {}", dnsItem1DomainName + ".ton", addrWallet);

    Address siteAddress = (Address) dns.getSiteAddress(dnsItem1DomainName + ".ton");
    log.info("{} resolved to {}", dnsItem1DomainName + ".ton", siteAddress);
  }

  private void getDnsCollectionInfo(Tonlib tonlib, Address dnsCollectionAddres) {
    CollectionData data = DnsCollection.getCollectionData(tonlib, dnsCollectionAddres);
    log.info("dns collection info {}", data);
  }

  private void getDnsCollectionInfo(TonCenter tonCenter, Address dnsCollectionAddres) {
    CollectionData data = DnsCollection.getCollectionData(tonCenter, dnsCollectionAddres);
    log.info("dns collection info {}", data);
  }

  private static void resolveDnsCollectionItems(Address dnsCollectionAddress) {
    CellBuilder cellApple = CellBuilder.beginCell();
    cellApple.storeString("apple");
    String hashApple = Utils.bytesToHex(cellApple.endCell().hash());
    log.info("apple hash {}", hashApple);

    CellBuilder cell3Alices = CellBuilder.beginCell();
    cell3Alices.storeString("alice-alice-alice");
    String hash3Alices = Utils.bytesToHex(cell3Alices.endCell().hash());
    log.info("alice-alice-alice hash {}", hash3Alices);

    CellBuilder cellAlices = CellBuilder.beginCell();
    cellAlices.storeString(
        "alicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicea");
    String hashAlices = Utils.bytesToHex(cellAlices.endCell().hash());
    log.info(
        "alices hash {}",
        hashAlices); // 8b98cad1bf9de7e1bd830ba3fba9608e6190825dddcf7edac7851ee16a692e81

    Address apple =
        DnsCollection.getNftItemAddressByIndex(
            tonlib, dnsCollectionAddress, new BigInteger(hashApple, 16));
    Address alice3 =
        DnsCollection.getNftItemAddressByIndex(
            tonlib, dnsCollectionAddress, new BigInteger(hash3Alices, 16));
    Address aliceX =
        DnsCollection.getNftItemAddressByIndex(
            tonlib, dnsCollectionAddress, new BigInteger(hashAlices, 16));

    log.info(
        "address at index hash(apple)             {} = {}",
        hashApple,
        apple.toString(true, true, true));
    log.info(
        "address at index hash(alice-alice-alice) {} = {}",
        hash3Alices,
        alice3.toString(true, true, true));
    log.info(
        "address at index hash(alice...)          {} = {}",
        hashAlices,
        aliceX.toString(true, true, true));

    Address appleAddress =
        (Address)
            DnsCollection.resolve(
                tonlib, dnsCollectionAddress, "apple", DNS_CATEGORY_NEXT_RESOLVER, true);
    log.info("apple resolved to {}", appleAddress.toString(true, true, true));

    Address alice3Resolved =
        (Address)
            DnsCollection.resolve(
                tonlib,
                dnsCollectionAddress,
                "alice-alice-alice",
                DNS_CATEGORY_NEXT_RESOLVER,
                true);
    log.info("alice-alice-alice resolved to {}", alice3Resolved.toString(true, true, true));

    Address alices =
        (Address)
            DnsCollection.resolve(
                tonlib,
                dnsCollectionAddress,
                "alicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicea",
                DNS_CATEGORY_NEXT_RESOLVER,
                true);
    log.info("alice... resolved to {}", alices.toString(true, true, true));
  }

  private void getDnsItemInfo(Tonlib tonlib, Address dnsItemAddress) {
    ItemData data = DnsCollection.getNftItemContent(tonlib, dnsItemAddress);
    log.info("dns item data {}", data);
    log.info(
        "dns item collection address {}", data.getCollectionAddress().toString(true, true, true));
    if (nonNull(data.getOwnerAddress())) {
      log.info("dns item owner address {}", data.getOwnerAddress().toString(true, true, true));
    }

    if (isNull(data.getOwnerAddress())) {
      AuctionInfo auctionInfo = DnsItem.getAuctionInfo(tonlib, dnsItemAddress);
      Address maxBidAddress = auctionInfo.getMaxBidAddress();
      BigInteger maxBidAmount = auctionInfo.getMaxBidAmount();
      log.info(
          "AUCTION: maxBid {}, maxBidAddress {}, endTime {}",
          Utils.formatNanoValue(maxBidAmount),
          maxBidAddress.toString(true, true, true),
          Utils.toUTC(auctionInfo.getAuctionEndTime()));
    } else {
      log.info("SOLD to {}", data.getOwnerAddress().toString(true, true, true));
    }
    String domain = DnsItem.getDomain(tonlib, dnsItemAddress);
    log.info("domain {}", domain);

    long lastFillUpTime = DnsItem.getLastFillUpTime(tonlib, dnsItemAddress);
    log.info("lastFillUpTime {}, {}", lastFillUpTime, Utils.toUTC(lastFillUpTime));
  }

  private void getDnsItemInfo(TonCenter tonCenter, Address dnsItemAddress) {
    ItemData data = DnsCollection.getNftItemContent(tonCenter, dnsItemAddress);
    log.info("dns item data {}", data);
    log.info(
            "dns item collection address {}", data.getCollectionAddress().toString(true, true, true));
    if (nonNull(data.getOwnerAddress())) {
      log.info("dns item owner address {}", data.getOwnerAddress().toString(true, true, true));
    }

    if (isNull(data.getOwnerAddress())) {
      AuctionInfo auctionInfo = DnsItem.getAuctionInfo(tonCenter, dnsItemAddress);
      Address maxBidAddress = auctionInfo.getMaxBidAddress();
      BigInteger maxBidAmount = auctionInfo.getMaxBidAmount();
      log.info(
              "AUCTION: maxBid {}, maxBidAddress {}, endTime {}",
              Utils.formatNanoValue(maxBidAmount),
              maxBidAddress.toString(true, true, true),
              Utils.toUTC(auctionInfo.getAuctionEndTime()));
    } else {
      log.info("SOLD to {}", data.getOwnerAddress().toString(true, true, true));
    }
    String domain = DnsItem.getDomain(tonCenter, dnsItemAddress);
    log.info("domain {}", domain);

    long lastFillUpTime = DnsItem.getLastFillUpTime(tonCenter, dnsItemAddress);
    log.info("lastFillUpTime {}, {}", lastFillUpTime, Utils.toUTC(lastFillUpTime));
  }

  private SendResponse changeDnsRecord(
      WalletV3R1 ownerWallet, Address dnsItemAddress, Address newSmartContract) {
    Cell body =
        DnsItem.createChangeContentEntryBody(
            DNS_CATEGORY_WALLET, DnsUtils.createSmartContractAddressRecord(newSmartContract), 0);

    WalletV3Config ownerWalletConfig =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(ownerWallet.getSeqno())
            .destination(dnsItemAddress)
            .amount(Utils.toNano(0.07))
            .body(body)
            .build();

    return ownerWallet.send(ownerWalletConfig);
  }

  private SendResponse transferDnsItem(
      WalletV3R1 ownerWallet, Address dnsItemAddress, String newOwner) {
    WalletV3Config ownerWalletConfig =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(ownerWallet.getSeqno())
            .destination(dnsItemAddress)
            .amount(Utils.toNano(0.07))
            .body(
                DnsItem.createTransferBody(
                    0,
                    Address.of(newOwner),
                    Utils.toNano(0.02),
                    "gift".getBytes(),
                    ownerWallet.getAddress()))
            .build();

    return ownerWallet.send(ownerWalletConfig);
  }

  private SendResponse releaseDnsItem(
      WalletV3R1 ownerWallet, Address dnsItemAddress, BigInteger amount) {
    Cell body =
        CellBuilder.beginCell()
            .storeUint(0x4ed14b65, 32) // op::dns_balance_release = 0x4ed14b65;
            .storeUint(123, 64)
            .endCell();
    WalletV3Config ownerWalletConfig =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(ownerWallet.getSeqno())
            .destination(dnsItemAddress)
            .amount(amount)
            .body(body)
            .build();

    return ownerWallet.send(ownerWalletConfig);
  }

  private static SendResponse governDnsItem(WalletV3R1 ownerWallet, Address dnsItemAddress) {
    Cell body =
        CellBuilder.beginCell()
            .storeUint(0x44beae41, 32) // op::process_governance_decision = 0x44beae41;
            .storeUint(123, 64)
            .endCell();
    WalletV3Config ownerWalletConfig =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(ownerWallet.getSeqno())
            .destination(dnsItemAddress)
            .amount(Utils.toNano(1))
            .body(body)
            .build();

    return ownerWallet.send(ownerWalletConfig);
  }

  private static SendResponse getStaticData(WalletV3R1 ownerWallet, Address dnsItem1Address) {
    WalletV3Config ownerWalletConfig =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(ownerWallet.getSeqno())
            .destination(dnsItem1Address)
            .amount(Utils.toNano(0.05))
            .body(DnsItem.createStaticDataBody(661))
            .build();

    return ownerWallet.send(ownerWalletConfig);
  }
}
