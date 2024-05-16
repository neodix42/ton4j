package org.ton.java.smartcontract.payments;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.mnemonic.Ed25519;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.ChannelData;
import org.ton.java.smartcontract.types.ChannelState;
import org.ton.java.smartcontract.types.FromWalletConfig;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;

import java.math.BigInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.ton.java.smartcontract.payments.PaymentsUtils.*;

public class PaymentChannel implements Contract<FromWalletConfig> {

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
            options.code = CellBuilder.beginCell().fromBoc(WalletCodes.payments.getValue()).endCell();
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
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeBit(false); // inited
        cell.storeCoins(BigInteger.ZERO); // balance_A
        cell.storeCoins(BigInteger.ZERO); // balance_B
        cell.storeBytes(getOptions().publicKeyA);
        cell.storeBytes(getOptions().publicKeyB);
        cell.storeUint(getOptions().getChannelConfig().getChannelId(), 128); // channel_id

        CellBuilder closingConfig = CellBuilder.beginCell();
        if (nonNull(getOptions().getClosingConfig())) {
            closingConfig.storeUint(getOptions().getClosingConfig().quarantineDuration, 32); // quarantine_duration
            closingConfig.storeCoins(isNull(getOptions().getClosingConfig().misbehaviorFine) ? BigInteger.ZERO : getOptions().getClosingConfig().misbehaviorFine); // misbehavior_fine
            closingConfig.storeUint(getOptions().getClosingConfig().conditionalCloseDuration, 32); // conditional_close_duration
        } else {
            closingConfig.storeUint(0, 32); // quarantine_duration
            closingConfig.storeCoins(BigInteger.ZERO); // misbehavior_fine
            closingConfig.storeUint(0, 32); // conditional_close_duration
        }
        cell.storeRef(closingConfig.endCell());

        cell.storeUint(0, 32); // committed_seqno_A
        cell.storeUint(0, 32); // committed_seqno_B
        cell.storeBit(false); // quarantine ref

        CellBuilder paymentConfig = CellBuilder.beginCell()
                .storeCoins(isNull(getOptions().excessFee) ? BigInteger.ZERO : getOptions().excessFee) // excess_fee
                .storeAddress(getOptions().getChannelConfig().getAddressA()) // addr_A
                .storeAddress(getOptions().getChannelConfig().getAddressB()); // addr_B

        cell.storeRef(paymentConfig.endCell());

        return cell.endCell();
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.payments.getValue()).
                endCell();
    }

    @Override
    public Cell createTransferBody(FromWalletConfig config) {
        return null;
    }


    public ExtMessageInfo deploy(Tonlib tonlib, FromWalletConfig config) {
        return null;
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
        Cell cell = PaymentsUtils.createCooperativeCloseChannelBody(
                getOptions().getChannelConfig().getChannelId(),
                channelState.getBalanceA(),
                channelState.getBalanceB(),
                channelState.getSeqnoA(),
                channelState.getSeqnoB());
        return Ed25519.verify(getOptions().isA ? getOptions().publicKeyB : getOptions().publicKeyA, cell.hash(), hisSignature);
    }

    public Signature createStartUncooperativeClose(Cell signedSemiChannelStateA, Cell signedSemiChannelStateB) {
        return this.createOneSignature(op_start_uncooperative_close,
                createStartUncooperativeCloseBody(
                        getOptions().getChannelConfig().getChannelId(),
                        signedSemiChannelStateA,
                        signedSemiChannelStateB));
    }

    public Signature createChallengeQuarantinedState(Cell signedSemiChannelStateA, Cell signedSemiChannelStateB) {
        return this.createOneSignature(op_challenge_quarantined_state,
                createChallengeQuarantinedStateBody(
                        getOptions().getChannelConfig().getChannelId(),
                        signedSemiChannelStateA,
                        signedSemiChannelStateB));
    }

    public Signature createSettleConditionals(Cell conditionalsToSettle) {
        return this.createOneSignature(op_settle_conditionals,
                createSettleConditionalsBody(
                        getOptions().getChannelConfig().getChannelId(),
                        conditionalsToSettle));
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
            quarantine = CellBuilder.beginCell().fromBoc(t.getCell().getBytes()).endCell();
        }

        TvmStackEntryTuple trippleTuple = (TvmStackEntryTuple) result.getStack().get(7);
        TvmStackEntryNumber excessFee = (TvmStackEntryNumber) trippleTuple.getTuple().getElements().get(0);

        TvmStackEntryCell addressACell = (TvmStackEntryCell) trippleTuple.getTuple().getElements().get(1);

        Address addressA = NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(addressACell.getCell().getBytes()).endCell());

        TvmStackEntryCell AddressBCell = (TvmStackEntryCell) trippleTuple.getTuple().getElements().get(2);

        Address addressB = NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(AddressBCell.getCell().getBytes()).endCell());

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

    public FromWallet fromWallet(Tonlib tonlib, Contract wallet, byte[] secretKey) {
        return new FromWallet(tonlib, wallet, secretKey, this.options);
    }
}