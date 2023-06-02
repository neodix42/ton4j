package org.ton.java.smartcontract.payments;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.mnemonic.Ed25519;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.ChannelData;
import org.ton.java.smartcontract.types.ChannelState;
import org.ton.java.smartcontract.types.WalletCodes;
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
            options.code = Cell.fromBoc(WalletCodes.payments.getValue());
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
        if (isNull(this.address)) {
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
        byte[] signature = new TweetNaclFast.Signature(getOptions().myKeyPair.getPublicKey(), getOptions().myKeyPair.getSecretKey()).detached(Utils.unsignedBytesToSigned(cellForSigning.hash()));

        Cell cell = PaymentsUtils.createOneSignature(op, getOptions().isA, signature, cellForSigning);

        return Signature.builder().cell(cell).signature(signature).build();
    }

    public Signature createTwoSignature(long op, byte[] hisSignature, Cell cellForSigning) {
        byte[] signature = new TweetNaclFast.Signature(getOptions().myKeyPair.getPublicKey(), getOptions().myKeyPair.getSecretKey()).detached(Utils.unsignedBytesToSigned(cellForSigning.hash()));

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

        byte[] signature = new TweetNaclFast.Signature(getOptions().myKeyPair.getPublicKey(), getOptions().myKeyPair.getSecretKey()).detached(Utils.unsignedBytesToSigned(state.hash()));
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

        return Ed25519.verify(getOptions().isA ? getOptions().publicKeyB : getOptions().publicKeyA, Utils.unsignedBytesToSigned(state.hash()), hisSignature);
    }

    public byte[] signClose(ChannelState channelState) {
        Signature s = this.createCooperativeCloseChannel(null, channelState);
        return s.signature;
    }

    public boolean verifyClose(ChannelState channelState, byte[] hisSignature) {
        Cell cell = PaymentsUtils.createCooperativeCloseChannelBody(getOptions().getChannelConfig().getChannelId(),
                channelState.getBalanceA(), channelState.getBalanceB(), channelState.getSeqnoA(), channelState.getSeqnoB());
        return Ed25519.verify(getOptions().isA ? getOptions().publicKeyB : getOptions().publicKeyA, Utils.unsignedBytesToSigned(cell.hash()), hisSignature);
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
            quarantine = CellBuilder.fromBoc(Utils.base64ToUnsignedBytes(t.getCell().getBytes()));
        }

        TvmStackEntryTuple trippleTuple = (TvmStackEntryTuple) result.getStack().get(7);
        TvmStackEntryNumber excessFee = (TvmStackEntryNumber) trippleTuple.getTuple().getElements().get(0);

        TvmStackEntryCell addressACell = (TvmStackEntryCell) trippleTuple.getTuple().getElements().get(1);

        Address addressA = NftUtils.parseAddress(CellBuilder.fromBoc(Utils.base64ToUnsignedBytes(addressACell.getCell().getBytes())));

        TvmStackEntryCell AddressBCell = (TvmStackEntryCell) trippleTuple.getTuple().getElements().get(2);

        Address addressB = NftUtils.parseAddress(CellBuilder.fromBoc(Utils.base64ToUnsignedBytes(AddressBCell.getCell().getBytes())));

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