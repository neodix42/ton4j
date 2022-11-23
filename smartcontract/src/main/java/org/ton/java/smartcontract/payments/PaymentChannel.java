package org.ton.java.smartcontract.payments;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.mnemonic.Ed25519;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.ChannelData;
import org.ton.java.smartcontract.types.ChannelState;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.ton.java.smartcontract.payments.PaymentsUtils.*;

public class PaymentChannel implements WalletContract {
    public static final String PAYMENT_CHANNEL_CODE_HEX = "B5EE9C72410230010007FB000114FF00F4A413F4BCF2C80B0102012002030201480405000AF26C21F0190202CB06070201202E2F020120080902012016170201200A0B0201200C0D0009D3610F80CC001D6B5007434C7FE8034C7CC1BC0FE19E0201580E0F0201201011002D3E11DBC4BE11DBC43232C7FE11DBC47E80B2C7F2407320008B083E1B7B51343480007E187E80007E18BE80007E18F4FFC07E1934FFC07E1974DFC07E19BC01887080A7F4C7C07E1A34C7C07E1A7D01007E1AB7807080E535007E1AF7BE1B2002012012130201201415008D3E13723E11BE117E113E10540132803E10BE80BE10FE8084F2FFC4B2FFF2DFFC02887080A7FE12BE127E121400F2C7C4B2C7FD0037807080E53E12C073253E1333C5B8B27B5520004D1C3C02FE106CFCB8193E803E800C3E1096283E18BE10C0683E18FE10BE10E8006EFCB819BC032000CF1D3C02FE106CFCB819348020C235C6083E4040E4BE1124BE117890CC3E443CB81974C7C060841A5B9A5D2EBCB81A3E118074DFD66EBCB81CBE803E800C3E1094882FBE10D4882FAC3CB819807E18BE18FE12F43E800C3E10BE10E80068006E7CB8199FFE187C0320004120843777222E9C20043232C15401B3C594013E808532DA84B2C7F2DFF2407EC02002012018190201D42B2C0201201A1B0201201E1F0201201C1D00E5473F00BD401D001D401D021F90102D31F01821043436D74BAF2E068F84601D37F59BAF2E072F844544355F910F8454330F910B0F2E065D33FD33F30F84822B9F84922B9B0F2E06C21F86820F869F84A6E915B8E19F84AD0D33FFA003171D721D33F305033BC02BCB1936DF86ADEE2F800F00C8006F3E12F43E800C7E903E900C3E09DBC41CBE10D62F24CC20C1B7BE10FE11963C03FE10BE11A04020BC03DC3E185C3E189C3E18DB7E1ABC032000B51D3C02F5007400750074087E4040B4C7C0608410DB1BDCEEBCB81A3E118074DFD66EBCB81CBE111510D57E443E1150CC3E442C3CB8197E80007E18BE80007E18F4CFF4CFCC3E1208AE7E1248AE6C3CB81B007E1A3E1A7E003C042001C1573F00BF84A6EF2E06AD2008308D71820F9012392F84492F845E24130F910F2E065D31F018210556E436CBAF2E068F84601D37F59BAF2E072D401D08308D71820F901F8444130F910F2E06501D430D08308D71820F901F8454130F910F2E06501820020120222301FED31F01821043685374BAF2E068F84601D37F59BAF2E072D33FFA00F404552003D200019AD401D0D33FFA00F40430937F206DE2303205D31F01821043685374BAF2E068F84601D37F59BAF2E072D33FFA00F404552003D200019AD401D0D33FFA00F40430937F206DE23032F8485280BEF8495250BEB0524BBE1AB0527ABE19210064B05215BE14B05248BE17B0F2E06970F82305C8CB3F5004FA0215F40015CB3F5004FA0212F400CB1F12CA00CA00C9F86AF00C01C31CFC02FE129BACFCB81AF48020C235C6083E4048E4BE1124BE1178904C3E443CB81974C7C0608410DA19D46EBCB81A3E118074DFD66EBCB81CB5007420C235C6083E407E11104C3E443CB81940750C3420C235C6083E407E11504C3E443CB81940602403F71CFC02FE129BACFCB81AF48020C235C6083E4048E4BE1124BE1178904C3E443CB81974C7C0608410DB10DBAEBCB81A3E118074DFD66EBCB81CBD010C3E12B434CFFE803D0134CFFE803D0134C7FE11DBC4148828083E08EE7CB81BBE11DBC4A83E08EF3CB81C34800C151D5A64D6D4C8F7A2B98E82A49B08B8C3816028292A01FCD31F01821043685374BAF2E068F84601D37F59BAF2E072D33FFA00F404552003D200019AD401D0D33FFA00F40430937F206DE2303205D31F01821043685374BAF2E068F84601D37F59BAF2E072D33FFA00F404552003D200019AD401D0D33FFA00F40430937F206DE230325339BE5381BEB0F8495250BEB0F8485290BEB02502FE5237BE16B05262BEB0F2E06927C20097F84918BEF2E0699137E222C20097F84813BEF2E0699132E2F84AD0D33FFA00F404D33FFA00F404D31FF8476F105220A0F823BCF2E06FD200D20030B3F2E073209C3537373A5274BC5263BC12B18E11323939395250BC5299BC18B14650134440E25319BAB3F2E06D9130E30D7F05C82627002496F8476F1114A098F8476F1117A00603E203003ECB3F5004FA0215F40012CB3F5004FA0213F400CB1F12CA00CA00C9F86AF00C00620A8020F4966FA5208E213050038020F4666FA1208E1001FA00ED1E15DA119450C3A00B9133E2923430E202926C21E2B31B000C3535075063140038C8CB3F5004FA0212F400CB3F5003FA0213F400CB1FCA00C9F86AF00C00D51D3C02FE129BACFCB81AFE12B434CFFE803D010C74CFFE803D010C74C7CC3E11DBC4283E11DBC4A83E08EE7CB81C7E003E10886808E87E18BE10D400E816287E18FE10F04026BE10BE10E83E189C3E18F7BE10B04026BE10FE10A83E18DC3E18F780693E1A293E1A7C042001F53B7EF4C7C8608419F1F4A06EA4CC7C037808608403818830AEA54C7C03B6CC780C882084155DD61FAEA54C3C0476CC780820841E6849BBEEA54C3C04B6CC7808208407C546B3EEA54C3C0576CC780820840223AA8CAEA54C3C05B6CC7808208419BDBC1A6EA54C3C05F6CC780C60840950CAA46EA53C0636CC78202D0008840FF2F00075BC7FE3A7805FC25E87D007D207D20184100D0CAF6A1EC7C217C21B7817C227C22B7817C237C23FC247C24B7817C2524C3B7818823881B22A021984008DBD0CABA7805FC20C8B870FC253748B8F07C256840206B90FD0018C020EB90FD0018B8EB90E98F987C23B7882908507C11DE491839707C23B788507C23B789507C11DE48B9F03A4331C4966";

    public static final long STATE_UNINITED = 0;
    public static final long STATE_OPEN = 1;
    public static final long STATE_CLOSURE_STARTED = 2;
    public static final long STATE_SETTLING_CONDITIONALS = 3;
    public static final long STATE_AWAITING_FINALIZATION = 4;

    Options options;
    Address address;

    /**
     * <a href="https://github.com/ton-blockchain/payment-channels">Payment Channels</a>
     *
     * @param options Options
     *                isA: boolean,
     *                channelId: BigInteger,
     *                myKeyPair: nacl.SignKeyPair,
     *                hisPublicKey: byte[],
     *                initBalanceA: BigInteger,
     *                initBalanceB: BigInteger,
     *                addressA: Address,
     *                addressB: Address,
     *                closingConfig (optional):
     *                {
     *                quarantineDuration: long,
     *                misbehaviorFine: BigInteger,
     *                conditionalCloseDuration: long
     *                },
     *                excessFee?: BigInteger
     */
    public PaymentChannel(Options options) {

        this.options = options;
        this.options.publicKeyA = options.isA ? options.myKeyPair.getPublicKey() : options.hisPublicKey;
        this.options.publicKeyB = !options.isA ? options.myKeyPair.getPublicKey() : options.hisPublicKey;

        if (nonNull(options.address)) {
            this.address = Address.of(options.address);
        }

        if (isNull(options.code)) {
            options.code = Cell.fromBoc(PAYMENT_CHANNEL_CODE_HEX);
        }
    }

    public String getName() {
        return "payments";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public Address getAddress() {
        if (this.address == null) {
            return (createStateInit()).address;
        }
        return this.address;
    }

    @Override
    public Cell createDataCell() {
        Cell cell = new Cell();
        cell.bits.writeBit(false); // inited
        cell.bits.writeCoins(BigInteger.ZERO); // balance_A
        cell.bits.writeCoins(BigInteger.ZERO); // balance_B
        writePublicKey(cell, getOptions().publicKeyA); // key_A
        writePublicKey(cell, getOptions().publicKeyB); // key_B
        cell.bits.writeUint(getOptions().getChannelConfig().getChannelId(), 128); // channel_id

        Cell closingConfig = new Cell();
        if (nonNull(getOptions().getClosingConfig())) {
            closingConfig.bits.writeUint(getOptions().getClosingConfig().quarantineDuration, 32); // quarantine_duration
            closingConfig.bits.writeCoins(isNull(getOptions().getClosingConfig().misbehaviorFine) ? BigInteger.ZERO : getOptions().getClosingConfig().misbehaviorFine); // misbehavior_fine
            closingConfig.bits.writeUint(getOptions().getClosingConfig().conditionalCloseDuration, 32); // conditional_close_duration
        } else {
            closingConfig.bits.writeUint(0, 32); // quarantine_duration
            closingConfig.bits.writeCoins(BigInteger.ZERO); // misbehavior_fine
            closingConfig.bits.writeUint(0, 32); // conditional_close_duration
        }
        cell.refs.add(closingConfig);

        cell.bits.writeUint(0, 32); // committed_seqno_A
        cell.bits.writeUint(0, 32); // committed_seqno_B
        cell.bits.writeBit(false); // quarantine ref

        Cell paymentConfig = new Cell();
        paymentConfig.bits.writeCoins(isNull(getOptions().excessFee) ? BigInteger.ZERO : getOptions().excessFee); // excess_fee
        paymentConfig.bits.writeAddress(getOptions().getChannelConfig().getAddressA()); // addr_A
        paymentConfig.bits.writeAddress(getOptions().getChannelConfig().getAddressB()); // addr_B
        cell.refs.add(paymentConfig);

        return cell;
    }

    public Signature createOneSignature(long op, Cell cellForSigning) {
        byte[] signature = new TweetNaclFast.Signature(getOptions().myKeyPair.getPublicKey(), getOptions().myKeyPair.getSecretKey()).detached(cellForSigning.hash());

        Cell cell = PaymentsUtils.createOneSignature(op, getOptions().isA, signature, cellForSigning);

        return Signature.builder().cell(cell).signature(signature).build();
    }

    public Signature createTwoSignature(long op, byte[] hisSignature, Cell cellForSigning) {
        byte[] signature = new TweetNaclFast.Signature(getOptions().myKeyPair.getPublicKey(), getOptions().myKeyPair.getSecretKey()).detached(cellForSigning.hash());

        byte[] signatureA = getOptions().isA ? signature : hisSignature;
        byte[] signatureB = !getOptions().isA ? signature : hisSignature;

        Cell cell = PaymentsUtils.createTwoSignature(op, signatureA, signatureB, cellForSigning);

        return Signature.builder().cell(cell).signature(signature).build();
    }

    public Cell createTopUpBalance(BigInteger coinsA, BigInteger coinsB) {
        return PaymentsUtils.createTopUpBalance(coinsA, coinsB);
    }

    public Signature createInitChannel(BigInteger balanceA, BigInteger balanceB) {
        return this.createOneSignature(op_init_channel, PaymentsUtils.createInitChannelBody(getOptions().getChannelConfig().getChannelId(), balanceA, balanceB));
    }

    public Signature createCooperativeCloseChannel(byte[] hisSignature, ChannelState channelState) {
        if (isNull(hisSignature)) {
            hisSignature = new byte[512 / 8];
        }
        return this.createTwoSignature(op_cooperative_close, hisSignature,
                PaymentsUtils.createCooperativeCloseChannelBody(
                        getOptions().getChannelConfig().getChannelId(),
                        channelState.getBalanceA(),
                        channelState.getBalanceB(),
                        channelState.getSeqnoA(),
                        channelState.getSeqnoB()));
    }

    public Signature createCooperativeCommit(byte[] hisSignature, BigInteger seqnoA, BigInteger seqnoB) {
        if (hisSignature.length != 0) {
            hisSignature = new byte[512 / 8];
        }
        return this.createTwoSignature(op_cooperative_close, hisSignature, PaymentsUtils.createCooperativeCommitBody(getOptions().getChannelConfig().getChannelId(), seqnoA, seqnoB));
    }

    public Signature createSignedSemiChannelState(BigInteger mySeqNo, BigInteger mySentCoins, BigInteger hisSeqno, BigInteger hisSentCoins) {
        Cell state = createSemiChannelState(
                getOptions().getChannelConfig().getChannelId(),
                createSemiChannelBody(mySeqNo, mySentCoins, null),
                isNull(hisSeqno) ? null : createSemiChannelBody(hisSeqno, hisSentCoins, null));

        byte[] signature = new TweetNaclFast.Signature(getOptions().myKeyPair.getPublicKey(), getOptions().myKeyPair.getSecretKey()).detached(state.hash());
        Cell cell = PaymentsUtils.createSignedSemiChannelState(signature, state);

        return Signature.builder()
                .signature(signature)
                .cell(cell)
                .build();
    }

    public byte[] signState(ChannelState channelState) {
        BigInteger mySeqno = getOptions().isA ? channelState.getSeqnoA() : channelState.getSeqnoB();
        BigInteger hisSeqno = !getOptions().isA ? channelState.getSeqnoA() : channelState.getSeqnoB();

        BigInteger sentCoinsA = getOptions().getChannelConfig().getInitBalanceA().compareTo(channelState.getBalanceA()) > 0 ? getOptions().getChannelConfig().getInitBalanceA().subtract(channelState.getBalanceA()) : BigInteger.ZERO;
        BigInteger sentCoinsB = getOptions().getChannelConfig().getInitBalanceB().compareTo(channelState.getBalanceB()) > 0 ? getOptions().getChannelConfig().getInitBalanceB().subtract(channelState.getBalanceB()) : BigInteger.ZERO;

        BigInteger mySentCoins = getOptions().isA ? sentCoinsA : sentCoinsB;
        BigInteger hisSentCoins = !getOptions().isA ? sentCoinsA : sentCoinsB;

        Signature s = createSignedSemiChannelState(mySeqno, mySentCoins, hisSeqno, hisSentCoins);
        return s.signature;
    }

    public boolean verifyState(ChannelState channelState, byte[] hisSignature) {
        BigInteger mySeqno = !getOptions().isA ? channelState.getSeqnoA() : channelState.getSeqnoB();
        BigInteger hisSeqno = getOptions().isA ? channelState.getSeqnoA() : channelState.getSeqnoB();

        BigInteger sentCoinsA = getOptions().getChannelConfig().getInitBalanceA().compareTo(channelState.getBalanceA()) > 0 ?
                getOptions().getChannelConfig().getInitBalanceA().subtract(channelState.getBalanceA()) : BigInteger.ZERO;
        BigInteger sentCoinsB = getOptions().getChannelConfig().getInitBalanceB().compareTo(channelState.getBalanceB()) > 0 ?
                getOptions().getChannelConfig().getInitBalanceB().subtract(channelState.getBalanceB()) : BigInteger.ZERO;

        BigInteger mySentCoins = !getOptions().isA ? sentCoinsA : sentCoinsB;
        BigInteger hisSentCoins = getOptions().isA ? sentCoinsA : sentCoinsB;

        Cell state = createSemiChannelState(
                getOptions().getChannelConfig().getChannelId(),
                createSemiChannelBody(mySeqno, mySentCoins, null),
                isNull(hisSeqno) ? null : createSemiChannelBody(hisSeqno, hisSentCoins, null));

        return Ed25519.verify(getOptions().isA ? getOptions().publicKeyB : getOptions().publicKeyA, state.hash(), hisSignature);
    }

    public byte[] signClose(ChannelState channelState) {
        Signature s = this.createCooperativeCloseChannel(null, channelState);
        return s.signature;
    }

    public boolean verifyClose(ChannelState channelState, byte[] hisSignature) {
        Cell cell = PaymentsUtils.createCooperativeCloseChannelBody(getOptions().getChannelConfig().getChannelId(),
                channelState.getBalanceA(), channelState.getBalanceB(), channelState.getSeqnoA(), channelState.getSeqnoB());
        return Ed25519.verify(getOptions().isA ? getOptions().publicKeyB : getOptions().publicKeyA, cell.hash(), hisSignature);
    }

    public Signature createStartUncooperativeClose(Cell signedSemiChannelStateA, Cell signedSemiChannelStateB) {
        return this.createOneSignature(op_start_uncooperative_close, createStartUncooperativeCloseBody(getOptions().getChannelConfig().getChannelId(), signedSemiChannelStateA, signedSemiChannelStateB));
    }

    public Signature createChallengeQuarantinedState(Cell signedSemiChannelStateA, Cell signedSemiChannelStateB) {
        return this.createOneSignature(op_challenge_quarantined_state, createChallengeQuarantinedStateBody(getOptions().getChannelConfig().getChannelId(), signedSemiChannelStateA, signedSemiChannelStateB));
    }

    public Signature createSettleConditionals(Cell conditionalsToSettle) {
        return this.createOneSignature(op_settle_conditionals, createSettleConditionalsBody(getOptions().getChannelConfig().getChannelId(), conditionalsToSettle));
    }

    public Cell createFinishUncooperativeClose() {
        return PaymentsUtils.createFinishUncooperativeClose();
    }

    public BigInteger getChannelState(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        System.out.println("get state my address " + myAddress.toString(true, true, true));
        RunResult result = tonlib.runMethod(myAddress, "get_channel_state");

        if (result.getExit_code() != 0) {
            throw new Error("method get_channel_state, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber addr = (TvmStackEntryNumber) result.getStack().get(0);
        return addr.getNumber();
    }

    public ChannelData getData(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        System.out.println("get data my address " + myAddress.toString(true, true, true));
        RunResult result = tonlib.runMethod(myAddress, "get_channel_data");

        if (result.getExit_code() != 0) {
            throw new Error("method get_channel_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber stateNumber = (TvmStackEntryNumber) result.getStack().get(0);

        TvmStackEntryTuple balanceTuple = (TvmStackEntryTuple) result.getStack().get(1);
        TvmStackEntryNumber balanceA = (TvmStackEntryNumber) balanceTuple.getTuple().getElements().get(0);
        TvmStackEntryNumber balanceB = (TvmStackEntryNumber) balanceTuple.getTuple().getElements().get(1);

        TvmStackEntryTuple keyTuple = (TvmStackEntryTuple) result.getStack().get(2);
        TvmStackEntryNumber publicKeyA = (TvmStackEntryNumber) keyTuple.getTuple().getElements().get(0);
        TvmStackEntryNumber publicKeyB = (TvmStackEntryNumber) keyTuple.getTuple().getElements().get(1);

        TvmStackEntryNumber channelIdNumber = (TvmStackEntryNumber) result.getStack().get(3);

        TvmStackEntryTuple closureConfigTuple = (TvmStackEntryTuple) result.getStack().get(4);
        TvmStackEntryNumber quarantineDuration = (TvmStackEntryNumber) closureConfigTuple.getTuple().getElements().get(0);
        TvmStackEntryNumber misbehaviourFine = (TvmStackEntryNumber) closureConfigTuple.getTuple().getElements().get(1);
        TvmStackEntryNumber conditionalCloseDuration = (TvmStackEntryNumber) closureConfigTuple.getTuple().getElements().get(2);

        TvmStackEntryTuple commitedSeqnoTuple = (TvmStackEntryTuple) result.getStack().get(5);
        TvmStackEntryNumber seqnoA = (TvmStackEntryNumber) commitedSeqnoTuple.getTuple().getElements().get(0);
        TvmStackEntryNumber seqnoB = (TvmStackEntryNumber) commitedSeqnoTuple.getTuple().getElements().get(1);

        Cell quarantine = null;
        TvmStackEntryList quarantineList = (TvmStackEntryList) result.getStack().get(6);
        for (Object o : quarantineList.getList().getElements()) {
            TvmStackEntryCell t = (TvmStackEntryCell) o;
            quarantine = CellBuilder.fromBoc(Utils.base64ToBytes(t.getCell().getBytes()));
        }

        TvmStackEntryTuple trippleTuple = (TvmStackEntryTuple) result.getStack().get(7);
        TvmStackEntryNumber excessFee = (TvmStackEntryNumber) trippleTuple.getTuple().getElements().get(0);

        TvmStackEntryCell addressACell = (TvmStackEntryCell) trippleTuple.getTuple().getElements().get(1);

        Address addressA = NftUtils.parseAddress(CellBuilder.fromBoc(Utils.base64ToBytes(addressACell.getCell().getBytes())));

        TvmStackEntryCell AddressBCell = (TvmStackEntryCell) trippleTuple.getTuple().getElements().get(2);

        Address addressB = NftUtils.parseAddress(CellBuilder.fromBoc(Utils.base64ToBytes(AddressBCell.getCell().getBytes())));

        return ChannelData.builder()
                .state(stateNumber.getNumber().longValue())
                .balanceA(balanceA.getNumber())
                .balanceB(balanceB.getNumber())
                .publicKeyA(publicKeyA.getNumber().toByteArray())
                .publicKeyB(publicKeyB.getNumber().toByteArray())
                .channelId(channelIdNumber.getNumber())
                .quarantineDuration(quarantineDuration.getNumber().longValue())
                .misbehaviorFine(misbehaviourFine.getNumber())
                .conditionalCloseDuration(conditionalCloseDuration.getNumber().longValue())
                .seqnoA(seqnoA.getNumber())
                .seqnoB(seqnoB.getNumber())
                .quarantine(quarantine)
                .excessFee(excessFee.getNumber())
                .addressA(addressA)
                .addressB(addressB)
                .build();
    }

    public FromWallet fromWallet(Tonlib tonlib, WalletContract wallet, byte[] secretKey) {
        return new FromWallet(tonlib, wallet, secretKey, this.options);
    }
}