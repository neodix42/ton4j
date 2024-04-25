package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.GenerateWallet;
import org.ton.java.smartcontract.TestWallet;
import org.ton.java.smartcontract.dns.Dns;
import org.ton.java.smartcontract.dns.DnsCollection;
import org.ton.java.smartcontract.dns.DnsItem;
import org.ton.java.smartcontract.dns.DnsUtils;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.*;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RawAccountState;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.ton.java.smartcontract.dns.Dns.DNS_CATEGORY_NEXT_RESOLVER;
import static org.ton.java.smartcontract.dns.Dns.DNS_CATEGORY_WALLET;

@Slf4j
@RunWith(JUnit4.class)
public class TestDns extends CommonTest {
    static TestWallet adminWallet;
    static TestWallet buyerWallet;

    static Tonlib tonlib = Tonlib.builder()
            .testnet(true)
            .ignoreCache(false)
            .build();


    @Test
    public void testDnsResolveTestnet() {
        Dns dns = new Dns(tonlib);
        Address dnsRootAddress = dns.getRootDnsAddress();
        log.info("root DNS address = {}", dnsRootAddress.toString(true, true, true));

        Object result = dns.resolve("apple.ton", DNS_CATEGORY_NEXT_RESOLVER, true);
        String resolvedAddress = ((Address) result).toString(true, true, true);
        log.info("apple.ton resolved to {}", resolvedAddress);
        assertThat(resolvedAddress).isNotEmpty();

        //item EQD9YWaIR_M_FIDQbNP6S8miv-3FU7kIHMRqg_S6bH_bDowf
        //owner EQAsEbAKNuRFDkoB6PjYP2dPTdHgt1rX2szkFFHahuDOEkbB
        // new owner EQBCMRzsJBTMDqF5JW8Mbq9Ap7b88qKxkwktlZEChtLbiFIH
//        Address addr = (Address) dns.getWalletAddress("alice-alice-alice-9.ton");
        Address addr = (Address) dns.resolve("alice-alice-alice-9.ton", DNS_CATEGORY_NEXT_RESOLVER, true);
        log.info("alice-alice-alice-9 resolved to {}", addr.toString(true, true, true));
    }

    @Test
    public void testDnsResolveMainnet() {
        Tonlib tonlib = Tonlib.builder().build();
        Dns dns = new Dns(tonlib);
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

        adminWallet = GenerateWallet.random(tonlib, 15);

        DnsRoot dnsRoot = new DnsRoot();
        log.info("new root DNS address {}", dnsRoot.getAddress().toString(true, true, true));

        dnsRoot.deploy(tonlib, adminWallet.getWallet(), adminWallet.getKeyPair());

        Utils.sleep(30);

        RawAccountState state;
        int i = 0;
        do {
            Utils.sleep(6);
            state = tonlib.getRawAccountState(dnsRoot.getAddress());
            if (i++ > 10) {
                throw new Error("time out getting account state");
            }
        } while (StringUtils.isEmpty(state.getCode()));

        log.info("root dns account state {}", state);
    }

    @Test
    public void testDnsCollectionItemDeploy() throws InterruptedException {

        adminWallet = GenerateWallet.random(tonlib, 15);
        buyerWallet = GenerateWallet.random(tonlib, 40);

        log.info("admin wallet address {}", adminWallet.getWallet().getAddress().toString(true, true, true));
        log.info("buyer wallet address {}", buyerWallet.getWallet().getAddress().toString(true, true, true));

        String dnsItemCodeHex = "B5EE9C7241022801000698000114FF00F4A413F4BCF2C80B0102016202030202CC04050201201E1F02012006070201481819020120080902015816170201200A0B000D470C8CB01C9D0801F73E09DBC400B434C0C05C6C2497C1383E903E900C7E800C5C75C87E800C7E800C3C0289ECE39397C15B088D148CB1C17CB865407E90350C1B5C3232C1FD00327E08E08418B9101A68608209E3402A4108308324CC200337A0404B20403C162A20032A41287E08C0683C00911DFC02440D7E08FC02F814D671C1462C200C00113E910C1C2EBCB8536003F88E34109B5F0BFA40307020F8256D8040708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00E029C70091709509D31F50AAE221F008F82321BC24C0008E9E343A3A3B8E1636363737375135C705F2E196102510241023F823F00BE30EE0310DD33F256EB31FB0926C21E30D0D0E0F00FE302680698064A98452B0BEF2E19782103B9ACA0052A0A15270BC993682103B9ACA0019A193390805E220C2008E328210557CEA20F82510396D71708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00923036E2803C23F823A1A120C2009313A0029130E24474F0091024F823F00B00D2343653CDA182103B9ACA005210A15270BC993682103B9ACA0016A1923005E220C2008E378210370FEC516D72295134544743708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB001CA10B9130E26D5477655477632EF00B0204C882105FCC3D145220BA8E9531373B5372C705F2E191109A104910384706401504E082101A0B9D515220BA8E195B32353537375135C705F2E19A03D4304015045033F823F00BE02182104EB1F0F9BAE3023B20821044BEAE41BAE302382782104ED14B65BA1310111200885B363638385147C705F2E19B04D3FF20D74AC20007D0D30701C000F2E19CF404300798D43040168307F417983050058307F45B30E270C8CB07F400C910354014F823F00B01FE30363A246EF2E19D8050F833D0F4043052408307F40E6FA1F2E19FD30721C00022C001B1F2E1A021C0008E9124109B1068517A10571046105C43144CDD9630103A395F07E201C0018E32708210370FEC51586D8100A0708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00915BE21301FE8E7A37F8235006A1810258BC066E16B0F2E19E23D0D749F823F0075290BEF2E1975178A182103B9ACA00A120C2008E32102782104ED14B6558076D72708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB0093303535E2F82381012CA0F0024477F0091045103412F823F00BE05F041501F03502FA4021F001FA40D20031FA0082103B9ACA001DA121945314A0A1DE22D70B01C300209205A19135E220C2FFF2E192218E3E821005138D91C8500BCF16500DCF1671244B145448C0708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00106994102C395BE20114008A8E3528F0018210D53276DB103946096D71708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB0093383430E21045103412F823F00B009A32353582102FCB26A2BA8E3A7082108B77173504C8CBFF5005CF161443308040708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00E05F04840FF2F00093083001258C2040FA201938083001658C20407D200CB8083001A58C204064200A38083001E58C20404B2007B8083002258C204032200538083002650C20191EB83002A4E00C9D781E9C600069006AC0BC018060840EE6B2802A0060840EE6B2802A00A08418B9101A68608209E3402A410830856456F81B04A5A9D6A0192A4139200201201A1B0201201C1D0021081BA50C1B5C0838343E903E8034CFCC200017321400F3C5807E80B2CFF26000513B513434FFFE900835D2708027DFC07E9035353D0134CFCC0415C415B80C1C1B5B5B5B490415C415A0002B01B232FFD40173C59400F3C5B3333D0032CFF27B5520020120202102012024250013BBB39F00A175F07F008802027422230010A874F00A10475F07000CA959F00A6C71000DB8FCFF00A5F03802012026270013B64A5E014204EBE0FA1000C7B461843AE9240F152118001E5C08DE014206EBE0FA1A60E038001E5C339E8086007AE140F8001E5C33B84111C466105E033E04883DCB11FB64DDC4964AD1BA06B879240DC23572F37CC5CAAAB143A2FFFBC4180012660F003C003060FE81EDF4260F00306EB1583C";
        String dnsCollectionCodeHex = "B5EE9C7241021D010002C7000114FF00F4A413F4BCF2C80B0102016202030202CC040502012017180201200607020120131402012008090201200D0E016D420C70094840FF2F0DE01D0D3030171B0925F03E0FA403001D31FED44D0D4D4303122C000E30210245F048210370FEC51BADC840FF2F080A0201200B0C00D032F82320821062E44069BCF2E0C701F00420D74920C218F2E0C8208103F0BBF2E0C92078A908C000F2E0CA21F005F2E0CB58F00714BEF2E0CC22F9018050F833206EB38E10D0F4043052108307F40E6FA131F2D0CD9130E2C85004CF16C9C85003CF1612CCC9F00C000D1C3232C072742000331C27C074C1C07000082CE500A98200B784B98C4830003CB432600201200F100201201112004F3223880875D244B5C61673C58875D2883000082CE6C070007CB83280B50C3400A44C78B98C727420007F1C0875D2638D572E882CE38B8C00B4C1C8700B48F0802C0929BE14902E6C08B08BC8F04EAC2C48B09800F05EC4EC04AC6CC82CE500A98200B784F7B99B04AEA00093083001258C2040FA201938083001658C20407D200CB8083001A58C204064200A38083001E58C20404B2007B8083002258C204032200538083002650C20191EB83002A4E00C9D781E9C600069006AC0BC018060840EE6B2802A0060840EE6B2802A00A08418B9101A68608209E3402A410830856456F81B04A5A9D6A0192A41392002015815160039D2CF8053810F805BBC00C646582AC678B387D0165B5E66664C0207D804002D007232FFFE0A33C5B25C083232C044FD003D0032C03260001B3E401D3232C084B281F2FFF27420020120191A0201201B1C0007B8B5D318001FBA7A3ED44D0D4D43031F00A7001F00B8001BB905BED44D0D4D430307FF002128009DBA30C3020D74978A908C000F2E04620D70A07C00021D749C0085210B0935B786DE0209501D3073101DE21F0035122D71830F9018200BA93C8CB0F01820167A3ED43D8CF16C90191789170E212A0018F83DF327";


        Options optionsDnsCollection = Options.builder()
                .collectionContent(NftUtils.createOffchainUriCell("https://raw.githubusercontent.com/neodiX42/ton4j/dns-smc/1-media/dns-collection-2.json"))
                .dnsItemCodeHex(dnsItemCodeHex)
                .code(CellBuilder.beginCell().fromBoc(dnsCollectionCodeHex).endCell())
                .build();

        Wallet dnsCollectionWallet = new Wallet(WalletVersion.dnsCollection, optionsDnsCollection);
        DnsCollection dnsCollection = dnsCollectionWallet.create();
        log.info("DNS collection address {}", dnsCollection.getAddress().toString(true, true, true));

        dnsCollection.deploy(tonlib, adminWallet.getWallet(), Utils.toNano(0.5), adminWallet.getKeyPair());
        Utils.sleep(30, "deploying dnsCollection");

        getDnsCollectionInfo(dnsCollection);

        resolveDnsCollectionItems(dnsCollection);

        String dnsItem1DomainName = "alice-alice-alice-9"; // do not add .ton, it will fail with error code 203
        // create and deploy DNS Item
        deployDnsItem(tonlib, adminWallet.getWallet(), Utils.toNano(10), dnsCollection.getAddress(), dnsItem1DomainName, adminWallet.getKeyPair());
        Utils.sleep(30, "deploying DNS item " + dnsItem1DomainName);

        Address dnsItem1Address = dnsCollection.getNftItemAddressByDomain(tonlib, dnsItem1DomainName);
        DnsItem dnsItem1 = new DnsItem(Options.builder().address(dnsItem1Address).build());
        log.info("dnsItem1 address {}", dnsItem1.getAddress().toString(true, true, true));

        getDnsItemInfo(dnsCollection, dnsItem1);

        //make a bid
        buyerWallet.getWallet().sendTonCoins(tonlib, buyerWallet.getKeyPair().getSecretKey(), dnsItem1Address, Utils.toNano(13));
        Utils.sleep(30, "bidding for DNS item " + dnsItem1DomainName);

        Address dnsItem1Editor = dnsItem1.getEditor(tonlib);
        log.info("dnsItem1 editor {}", nonNull(dnsItem1Editor) ? dnsItem1Editor.toString(true, true, true) : null);

        Utils.sleep(60 * 4, "wait till auction is finished");

        getDnsItemInfo(dnsCollection, dnsItem1);

        // claim your domain by doing any action with it
        getStaticData(adminWallet, dnsItem1);
        Utils.sleep(30, "Claim DNS item " + dnsItem1DomainName);
        //or assign your wallet to it, so it could resolove your wallet address to your-domain.ton
        changeDnsRecord(buyerWallet, dnsItem1, buyerWallet.getWallet().getAddress());
        Utils.sleep(30, "Claim DNS item " + dnsItem1DomainName);

        getDnsItemInfo(dnsCollection, dnsItem1);
        dnsItem1Editor = dnsItem1.getEditor(tonlib);
        log.info("dnsItem1 editor {}", nonNull(dnsItem1Editor) ? dnsItem1Editor.toString(true, true, true) : null);

        governDnsItem(buyerWallet, dnsItem1);
        Utils.sleep(30, "govern dns item");

        transferDnsItem(buyerWallet, dnsItem1, "EQBCMRzsJBTMDqF5JW8Mbq9Ap7b88qKxkwktlZEChtLbiFIH");
        Utils.sleep(30, "transferring domain to other user");

        // release of domain is possible if either one year has passed and auction has ended;
        // also msg_value >= min_price and obviously only the owner can release domain name;
        // once it is released the owner changed to null and auction starts again
        releaseDnsItem(buyerWallet, dnsItem1, Utils.toNano(15)); // will fail with error code 414
        Utils.sleep(30);

        getDnsItemInfo(dnsCollection, dnsItem1);
    }


    @Test
    public void testDnsItemDeployAtGlobalCollection() throws InterruptedException {

        adminWallet = GenerateWallet.random(tonlib, 15);
        log.info("admin wallet address {}", adminWallet.getWallet().getAddress().toString(true, true, true));
//        log.info("buyer wallet address {}", buyerWallet.getWallet().getAddress().toString(true, true, true));

        Options optionsDnsCollection = Options.builder()
                .address(Address.of("EQDjPtM6QusgMgWfl9kMcG-EALslbTITnKcH8VZK1pnH3UZA"))
                .collectionContent(NftUtils.createOffchainUriCell("https://raw.githubusercontent.com/neodiX42/ton4j/dns-smc/1-media/dns-collection.json"))
                .build();

        Wallet dnsCollectionWallet = new Wallet(WalletVersion.dnsCollection, optionsDnsCollection);
        DnsCollection dnsCollection = dnsCollectionWallet.create();
        log.info("DNS collection address {}", dnsCollection.getAddress().toString(true, true, true));

        getDnsCollectionInfo(dnsCollection);

        String dnsItem1DomainName = "alice-alice-alice-12";
        // create and deploy DNS Item
        deployDnsItem(tonlib, adminWallet.getWallet(), Utils.toNano(10), dnsCollection.getAddress(), dnsItem1DomainName, adminWallet.getKeyPair());
        Utils.sleep(30, "deploying DNS item " + dnsItem1DomainName);

        Address dnsItem1Address = dnsCollection.getNftItemAddressByDomain(tonlib, dnsItem1DomainName);
        DnsItem dnsItem1 = new DnsItem(Options.builder().address(dnsItem1Address).build());
        log.info("dnsItem1 address {}", dnsItem1.getAddress().toString(true, true, true));

        getDnsItemInfo(dnsCollection, dnsItem1);

        Utils.sleep(60 * 4, "wait till auction is finished");

        getDnsItemInfo(dnsCollection, dnsItem1);

        // claim your domain by doing any action with it
        getStaticData(adminWallet, dnsItem1);
        //assign your wallet to domain name
        changeDnsRecord(adminWallet, dnsItem1, adminWallet.getWallet().getAddress());
        Utils.sleep(25);

        getDnsItemInfo(dnsCollection, dnsItem1);

        Dns dns = new Dns(tonlib);
        Address dnsRootAddress = dns.getRootDnsAddress();
        log.info("root DNS address = {}", dnsRootAddress.toString(true, true, true));

        Address addrD = (Address) dns.resolve(dnsItem1DomainName + ".ton", DNS_CATEGORY_NEXT_RESOLVER, true);
        log.info("{} resolved to {}", dnsItem1DomainName + ".ton", addrD.toString(true, true, true));

        Address addrW = (Address) dns.getWalletAddress(dnsItem1DomainName + ".ton");
        log.info("{} resolved to {}", dnsItem1DomainName + ".ton", addrW.toString(true, true, true));
    }

    private long getDnsCollectionInfo(DnsCollection dnsCollection) {
        CollectionData data = dnsCollection.getCollectionData(tonlib);
        log.info("dns collection info {}", data);
        return data.getNextItemIndex();
    }

    private void resolveDnsCollectionItems(DnsCollection dnsCollection) {
        CellBuilder cellApple = CellBuilder.beginCell();
        cellApple.storeString("apple");
        String hashApple = Utils.bytesToHex(cellApple.endCell().hash());
        log.info("apple hash {}", hashApple);

        CellBuilder cell3Alices = CellBuilder.beginCell();
        cell3Alices.storeString("alice-alice-alice");
        String hash3Alices = Utils.bytesToHex(cell3Alices.endCell().hash());
        log.info("alice-alice-alice hash {}", hash3Alices);

        CellBuilder cellAlices = CellBuilder.beginCell();
        cellAlices.storeString("alicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicea");
        String hashAlices = Utils.bytesToHex(cellAlices.endCell().hash());
        log.info("alices hash {}", hashAlices); // 8b98cad1bf9de7e1bd830ba3fba9608e6190825dddcf7edac7851ee16a692e81

        Address apple = dnsCollection.getNftItemAddressByIndex(tonlib, new BigInteger(hashApple, 16));
        Address alice3 = dnsCollection.getNftItemAddressByIndex(tonlib, new BigInteger(hash3Alices, 16));
        Address aliceX = dnsCollection.getNftItemAddressByIndex(tonlib, new BigInteger(hashAlices, 16));

        log.info("address at index hash(apple)             {} = {}", hashApple, apple.toString(true, true, true));
        log.info("address at index hash(alice-alice-alice) {} = {}", hash3Alices, alice3.toString(true, true, true));
        log.info("address at index hash(alice...)          {} = {}", hashAlices, aliceX.toString(true, true, true));

        Address appleAddress = (Address) dnsCollection.resolve(tonlib, "apple", DNS_CATEGORY_NEXT_RESOLVER, true);
        log.info("apple resolved to {}", appleAddress.toString(true, true, true));

        Address alice3Resolved = (Address) dnsCollection.resolve(tonlib, "alice-alice-alice", DNS_CATEGORY_NEXT_RESOLVER, true);
        log.info("alice-alice-alice resolved to {}", alice3Resolved.toString(true, true, true));

        Address alices = (Address) dnsCollection.resolve(tonlib, "alicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicealicea", DNS_CATEGORY_NEXT_RESOLVER, true);
        log.info("alice... resolved to {}", alices.toString(true, true, true));
    }

    private void getDnsItemInfo(DnsCollection dnsCollection, DnsItem dnsItem) {
        ItemData data = dnsCollection.getNftItemContent(tonlib, dnsItem);
        log.info("dns item data {}", data);
        log.info("dns item collection address {}", data.getCollectionAddress().toString(true, true, true));
        if (nonNull(data.getOwnerAddress())) {
            log.info("dns item owner address {}", data.getOwnerAddress().toString(true, true, true));
        }

        if (isNull(data.getOwnerAddress())) {
            AuctionInfo auctionInfo = dnsItem.getAuctionInfo(tonlib);
            Address maxBidAddress = auctionInfo.getMaxBidAddress();
            BigInteger maxBidAmount = auctionInfo.getMaxBidAmount();
            log.info("AUCTION: maxBid {}, maxBidAddress {}, endTime {}", Utils.formatNanoValue(maxBidAmount), maxBidAddress.toString(true, true, true), Utils.toUTC(auctionInfo.getAuctionEndTime()));
        } else {
            log.info("SOLD to {}", data.getOwnerAddress().toString(true, true, true));
        }
        String domain = dnsItem.getDomain(tonlib);
        log.info("domain {}", domain);

        long lastFillUpTime = dnsItem.getLastFillUpTime(tonlib);
        log.info("lastFillUpTime {}, {}", lastFillUpTime, Utils.toUTC(lastFillUpTime));
    }

    private void changeDnsRecord(TestWallet ownerWallet, DnsItem dnsItem, Address newSmartContract) {
        long seqno = ownerWallet.getWallet().getSeqno(tonlib);

        ExternalMessage extMsg = ownerWallet.getWallet().createTransferMessage(
                ownerWallet.getKeyPair().getSecretKey(),
                dnsItem.getAddress(), // toAddress
                Utils.toNano(0.07),
                seqno,
                DnsItem.createChangeContentEntryBody(DNS_CATEGORY_WALLET,
                        DnsUtils.createSmartContractAddressRecord(newSmartContract),
                        0));

        tonlib.sendRawMessage(extMsg.message.toBase64());
    }

    private void transferDnsItem(TestWallet ownerWallet, DnsItem dnsItem, String newOwner) {
        long seqno = ownerWallet.getWallet().getSeqno(tonlib);

        ExternalMessage extMsg = ownerWallet.getWallet().createTransferMessage(
                ownerWallet.getKeyPair().getSecretKey(),
                dnsItem.getAddress(), // toAddress
                Utils.toNano(0.08),
                seqno,
                dnsItem.createTransferBody(
                        0,
                        Address.of(newOwner),
                        Utils.toNano(0.02),
                        "gift".getBytes(),
                        ownerWallet.getWallet().getAddress()));

        tonlib.sendRawMessage(extMsg.message.toBase64());
    }

    private void releaseDnsItem(TestWallet ownerWallet, DnsItem dnsItem, BigInteger amount) {
        long seqno = ownerWallet.getWallet().getSeqno(tonlib);

        CellBuilder payload = CellBuilder.beginCell();
        payload.storeUint(0x4ed14b65, 32); // op::dns_balance_release = 0x4ed14b65;
        payload.storeUint(123, 64);

        ExternalMessage extMsg = ownerWallet.getWallet().createTransferMessage(
                ownerWallet.getKeyPair().getSecretKey(),
                dnsItem.getAddress(), // toAddress
                amount,
                seqno,
                payload.endCell());

        tonlib.sendRawMessage(extMsg.message.toBase64());
    }

    private void governDnsItem(TestWallet ownerWallet, DnsItem dnsItem) {
        long seqno = ownerWallet.getWallet().getSeqno(tonlib);

        CellBuilder payload = CellBuilder.beginCell();
        payload.storeUint(0x44beae41, 32); // op::process_governance_decision = 0x44beae41;
        payload.storeUint(123, 64);

        ExternalMessage extMsg = ownerWallet.getWallet().createTransferMessage(
                ownerWallet.getKeyPair().getSecretKey(),
                dnsItem.getAddress(), // toAddress
                Utils.toNano(1),
                seqno,
                payload.endCell());

        tonlib.sendRawMessage(extMsg.message.toBase64());
    }

    private void getStaticData(TestWallet ownerWallet, DnsItem dnsItem) {
        long seqno = ownerWallet.getWallet().getSeqno(tonlib);

        ExternalMessage extMsg = ownerWallet.getWallet().createTransferMessage(
                ownerWallet.getKeyPair().getSecretKey(),
                dnsItem.getAddress(), // toAddress
                Utils.toNano(0.05),
                seqno,
                dnsItem.createStaticDataBody(661));

        tonlib.sendRawMessage(extMsg.message.toBase64());
    }

    private void deployDnsItem(Tonlib tonlib, WalletContract adminWallet, BigInteger msgValue, Address dnsCollectionAddress, String domainName, TweetNaclFast.Signature.KeyPair keyPair) {

        long seqno = adminWallet.getSeqno(tonlib);

        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(0, 32);  // OP deploy new nft
        body.storeRef(CellBuilder.beginCell().storeString(domainName).endCell());

        ExternalMessage extMsg = adminWallet.createTransferMessage(
                keyPair.getSecretKey(),
                dnsCollectionAddress,
                msgValue,
                seqno,
                body.endCell()
        );

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc()));
    }
}
