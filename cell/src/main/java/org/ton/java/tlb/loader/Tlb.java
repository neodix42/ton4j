package org.ton.java.tlb.loader;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.tlb.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.ton.java.tlb.types.Transaction.deserializeAccountState;

public class Tlb {

    public static Object load(Class clazz, Cell c) {
        if (isNull(c)) {
            return null;
        } else {
            return load(clazz, CellSlice.beginParse(c));
        }
    }

//    public static Object load(Class c, CellSlice cs) {
//        return load(c, cs);
//    }

    public static Object load(Class c, CellSlice cs) {
        long magic;
        switch (c.getSimpleName()) {
            case "BlockIdExt":
                return BlockIdExt.builder()
                        .workchain(cs.loadUint(32).longValue())
                        .shard(cs.loadUint(64).longValue())
                        .seqno(cs.loadUint(32).longValue())
                        .rootHash(cs.loadUint(256))
                        .fileHash(cs.loadUint(256))
                        .build();
            case "TickTock":
                return TickTock.builder()
                        .tick(cs.loadBit())
                        .tock(cs.loadBit())
                        .build();
            case "StateInit":
                return StateInit.builder()
                        .depth(cs.loadBit() ? cs.loadUint(5) : BigInteger.ZERO)
                        .tickTock(cs.loadBit() ? (TickTock) cs.loadTlb(TickTock.class) : null)
                        .code(cs.loadMaybeRefX())
                        .data(cs.loadMaybeRefX())
                        .lib(cs.loadDictE(256, k -> k.readInt(256), v -> v))
                        .build();
            case "AccountStorage":
                AccountStorage accountStorage = AccountStorage.builder().build();

                BigInteger lastTransaction = cs.loadUint(64);
                CurrencyCollection coins = (CurrencyCollection) cs.loadTlb(CurrencyCollection.class);

//                boolean extraExists = cs.loadBit();
//
//                if (extraExists) {
//                    throw new Error("extra currency info is not supported for AccountStorage");
//                }

                boolean isStatusActive = cs.loadBit();
                if (isStatusActive) {
                    accountStorage.setAccountStatus("ACTIVE");
                    StateInit stateInit = (StateInit) cs.loadTlb(StateInit.class);
                    accountStorage.setAccountState(
                            AccountStateActive.builder()
                                    .stateInit(stateInit)
                                    .build());
                } else {
                    boolean isStatusFrozen = cs.loadBit();
                    if (isStatusFrozen) {
                        accountStorage.setAccountStatus("FROZEN");
                        if (cs.getRestBits() != 0) {
                            BigInteger stateHash = cs.loadUint(256);
                            accountStorage.setAccountState(
                                    AccountStateFrozen.builder()
                                            .stateHash(stateHash)
                                            .build());
                        }
                    } else {
                        accountStorage.setAccountStatus("UNINIT");
                        accountStorage.setAccountState(
                                AccountStateUninit.builder().build());
                    }
                }
                accountStorage.setLastTransactionLt(lastTransaction);
                accountStorage.setBalance(coins);
                return accountStorage;
            case "StorageInfo":
                StorageUsed storageUsed = (StorageUsed) cs.loadTlb(StorageUsed.class);
                long lastPaid = cs.loadUint(32).longValue();
                boolean isDuePayment = cs.loadBit();
                return StorageInfo.builder()
                        .storageUsed(storageUsed)
                        .lastPaid(lastPaid)
                        .duePayment(isDuePayment ? cs.loadUint(64) : null)
                        .build();
            case "StorageUsed":
                BigInteger cells = cs.loadVarUInteger(BigInteger.valueOf(7));
                BigInteger bits = cs.loadVarUInteger(BigInteger.valueOf(7));
                BigInteger pubCells = cs.loadVarUInteger(BigInteger.valueOf(7));
                return StorageUsed.builder()
                        .bitsUsed(bits)
                        .cellsUsed(cells)
                        .publicCellsUsed(pubCells)
                        .build();
            case "Account": {
                boolean isAccount = cs.loadBit();
                if (!isAccount) {
                    return Account.builder().isNone(true).build();

                }
                MsgAddressInt address = (MsgAddressInt) cs.loadTlb(MsgAddressInt.class);
                StorageInfo info = (StorageInfo) cs.loadTlb(StorageInfo.class);
                AccountStorage storage = (AccountStorage) cs.loadTlb(AccountStorage.class);

                return Account.builder()
                        .isNone(false)
                        .address(address)
                        .storageInfo(info)
                        .accountStorage(storage)
                        .build();
            }
            case "AccountStatus":
                byte status = cs.loadUint(2).byteValueExact();
                switch (status) {
                    case 0b11 -> {
                        return "NON_EXIST";
                    }
                    case 0b10 -> {
                        return "ACTIVE";
                    }
                    case 0b01 -> {
                        return "FROZEN";
                    }
                    case 0b00 -> {
                        return "UNINIT";
                    }
                }
            case "StateUpdate":
                System.out.println("stateUpdate " + cs.sliceToCell().toHex());
                return StateUpdate.builder()
                        .oldHash(cs.loadUint(256))
                        .newHash(cs.loadUint(256))
                        .oldShardState((ShardState) Tlb.load(ShardState.class, CellSlice.beginParse(cs.loadRef())))
                        .newShardState((ShardState) Tlb.load(ShardState.class, CellSlice.beginParse(cs.loadRef())))
                        .build();
            case "ConfigParams":
                return ConfigParams.builder()
                        .configAddr(Address.of((byte) 0x11, 255, cs.loadBits(256).toByteArray())) // TODO prio 1
                        .config(CellSlice.beginParse(cs.loadRef()).loadDict(32, k -> k.readUint(32), v -> v))
                        .build();
            case "EnqueuedMsg": {
                return EnqueuedMsg.builder()
                        .enqueuedLt(cs.loadUint(64))
                        .outMsg((MsgEnvelope) Tlb.load(MsgEnvelope.class, CellSlice.beginParse(cs.loadRef())))
                        .build();
            }
            case "ProcessedUpto": {
                return ProcessedUpto.builder()
                        .lastMsgLt(cs.loadUint(64))
                        .lastMsgHash(cs.loadUint(64))
                        .build();
            }
            case "OutMsgQueueInfo": {
                return OutMsgQueueInfo.builder()
                        .outMsgQueue(
                                cs.loadDictAugE(352,
                                        k -> k.readInt(352),
                                        v -> Tlb.load(EnqueuedMsg.class, CellSlice.beginParse(v)),
                                        e -> CellSlice.beginParse(e).loadUint(64)
                                ))
                        .processedInfo(
                                cs.loadDictE(96,
                                        k -> k.readInt(96),
                                        v -> Tlb.load(ProcessedUpto.class, CellSlice.beginParse(v))
                                ))
                        .ihrPendingInfo(
                                cs.loadDictE(320,
                                        k -> k.readInt(320),
                                        v -> CellSlice.beginParse(v).loadUint(64)
                                ))
                        .build();
            }
            case "ShardAccount": {
                return ShardAccount.builder()
                        .account((Account) Tlb.load(Account.class, CellSlice.beginParse(cs.loadRef())))
                        .lastTransHash(cs.loadUint(64))
                        .lastTransLt(cs.loadUint(64))
                        .build();
            }
            case "DepthBalanceInfo": {
                return DepthBalanceInfo.builder()
                        .depth(cs.loadUint(5).intValue()) // tlb #<= 60
                        .currencies((CurrencyCollection) cs.loadTlb(CurrencyCollection.class))
                        .build();
            }
            case "ShardStateInfo": {
                return ShardStateInfo.builder()
                        .overloadHistory(cs.loadUint(64))
                        .underloadHistory(cs.loadUint(64))
                        .totalBalance((CurrencyCollection) cs.loadTlb(CurrencyCollection.class))
                        .totalValidatorFees((CurrencyCollection) cs.loadTlb(CurrencyCollection.class))
                        .libraries(cs.loadDictE(256,
                                k -> k.readInt(256),
                                v -> CellSlice.beginParse(v).loadTlb(LibDescr.class)))
                        .masterRef(cs.loadBit() ? (ExtBlkRef) Tlb.load(ExtBlkRef.class, cs.loadRef()) : null)
                        .build();
            }
            case "ShardStateUnsplit": {
                ShardStateUnsplit s = ShardStateUnsplit.builder()
                        .magic(cs.loadUint(32).longValue())
                        .globalId(cs.loadUint(32).intValue())
                        .shardIdent((ShardIdent) cs.loadTlb(ShardIdent.class))
                        .seqno(cs.loadUint(32).longValue())
                        .vertSeqno(cs.loadUint(32).longValue())
                        .genUTime(cs.loadUint(32).longValue())
                        .genLT(cs.loadUint(64))
                        .minRefMCSeqno(cs.loadUint(32).longValue())
                        .outMsgQueueInfo((OutMsgQueueInfo) Tlb.load(OutMsgQueueInfo.class, CellSlice.beginParse(cs.loadRef())))
                        .beforeSplit(cs.loadBit())
                        .accounts(CellSlice.beginParse(cs.loadRef()).loadDictAugE(256,
                                k -> k.readInt(256),
                                v -> v.loadTlb(ShardAccount.class),
                                e -> e.loadTlb(DepthBalanceInfo.class)))
                        .shardStateInfo((ShardStateInfo) Tlb.load(ShardStateInfo.class, CellSlice.beginParse(cs.loadRef())))
                        .build();
//                s.setCustom(isNull(cs.preloadMaybeRefX()) ? null : (McStateExtra) Tlb.load(McStateExtra.class, cs.loadRef()));
                s.setCustom(cs.loadBit() ? (McStateExtra) Tlb.load(McStateExtra.class, cs.loadRef()) : null);
                //.custom(isNull(cs.preloadMaybeRefX()) ? null : (McStateExtra) Tlb.load(McStateExtra.class, cs.loadRef()))
                //.build();
            }
            case "ShardIdent":
                magic = cs.loadUint(2).longValue();
                assert (magic == 0L) : "ShardIdent: magic not equal to 0b00, found 0x" + Long.toHexString(magic);
                return ShardIdent.builder()
                        .magic(0L)
                        .prefixBits(cs.loadUint(6).longValue())
                        .workchain(cs.loadInt(32).longValue())
                        .shardPrefix(cs.loadUint(64))
                        .build();
            case "ShardDesc": // todo
                return ShardDesc.builder().build();
            case "ShardState":
                long tag = cs.preloadUint(32).longValue();
                if (tag == 0x5f327da5L) {
                    ShardStateUnsplit left, right;
                    left = (ShardStateUnsplit) Tlb.load(ShardStateUnsplit.class, cs.loadRef());
                    right = (ShardStateUnsplit) Tlb.load(ShardStateUnsplit.class, cs.loadRef());
                    return ShardState.builder()
                            .magic(tag)
                            .left(left)
                            .right(right)
                            .build();
                } else if (tag == 0x9023afe2L) {
                    return ShardState.builder()
                            .magic(tag)
                            .left((ShardStateUnsplit) cs.loadTlb(ShardStateUnsplit.class))
                            .build();
                } else {
                    throw new Error("unknown shardstate magic, found 0x" + Long.toHexString(tag));
                }
            case "McStateExtra":
                if (isNull(cs)) {
                    return null;
                }
                magic = cs.loadUint(16).longValue();
                assert (magic == 0xcc26L) : "McStateExtra: magic not equal to 0xcc26, found 0x" + Long.toHexString(magic);

                return McStateExtra.builder()
                        .magic(0xcc26L)
                        .shardHashes(cs.loadDictE(32, k -> k.readInt(32), v -> v)) // todo BinTree
                        .configParams((ConfigParams) cs.loadTlb(ConfigParams.class))
                        .info(cs.loadRef()) // todo parse
                        .globalBalance((CurrencyCollection) cs.loadTlb(CurrencyCollection.class))
                        .build();
            case "McBlockExtra":
                magic = cs.loadUint(16).longValue();
                assert (magic == 0xcca5L) : "McBlockExtra: magic not equal to 0xcca5, found 0x" + Long.toHexString(magic);

                boolean keyBlock = cs.loadBit();
                return McBlockExtra.builder()
                        .magic(0xcca5L)
                        .keyBlock(keyBlock)
                        .shardHashes(cs.loadDictE(32, k -> k.readInt(32), v -> v))
                        .shardFees(cs.loadDictAugE(92,
                                k -> k.readInt(92),
                                v -> v,
                                e -> e))
                        .more(cs.loadRef())
                        .config(keyBlock ? (ConfigParams) cs.loadTlb(ConfigParams.class) : null)
                        .build();
            case "CurrencyCollection":
                return CurrencyCollection.builder()
                        .coins(cs.loadCoins())
                        .extraCurrencies(cs.loadDictE(32,
                                k -> k.readUint(32),// todo read varuint32
                                v -> v))
                        .build();
            case "GlobalVersion":
                magic = cs.loadUint(8).longValue();
                assert (magic == 0xc4L) : "GlobalVersion: magic not equal to 0xc4, found 0x" + Long.toHexString(magic);

                return GlobalVersion.builder()
                        .magic(0xc4L)
                        .version(cs.loadUint(32).longValue())
                        .capabilities(cs.loadUint(64))
                        .build();
            case "ExtBlkRef":
                return ExtBlkRef.builder()
                        .endLt(cs.loadUint(64))
                        .seqno(cs.loadUint(32).intValue())
                        .rootHash(cs.loadUint(256))
                        .fileHash(cs.loadUint(256))
                        .build();
            case "ImportFees":
                return ImportFees.builder()
                        .feesCollected(cs.loadCoins())
                        .valueImported((CurrencyCollection) cs.loadTlb(CurrencyCollection.class))
                        .build();
            case "IntermediateAddress":
                if (!cs.loadBit()) {
                    return IntermediateAddressRegular.builder()
                            .use_dest_bits(cs.loadUint(7).intValue()) // todo test if 7 bits
                            .build();
                }
                if (!cs.loadBit()) {
                    return IntermediateAddressSimple.builder()
                            .workchainId(cs.loadUint(8).intValue())
                            .addrPfx(cs.loadUint(64))
                            .build();
                }
                return IntermediateAddressExt.builder()
                        .workchainId(cs.loadUint(32).intValue())
                        .addrPfx(cs.loadUint(64))
                        .build();
            case "MsgEnvelope":
                magic = cs.loadUint(4).longValue();
                assert (magic == 4) : "MsgEnvelope: magic not equal to 4, found 0x" + Long.toHexString(magic);

                return MsgEnvelope.builder()
                        .currAddr((IntermediateAddress) cs.loadTlb(IntermediateAddress.class))
                        .nextAddr((IntermediateAddress) cs.loadTlb(IntermediateAddress.class))
                        .fwdFeeRemaining(cs.loadCoins())
                        .msg((Message) Tlb.load(Message.class, cs.loadRef()))
                        .build();
            case "OutMsg": {
                int outMsgFlag = cs.loadUint(3).intValue();
                switch (outMsgFlag) {
                    case 0b000 -> {
                        return OutMsgExt.builder()
                                .msg((Message) Tlb.load(Message.class, cs.loadRef()))
                                .transaction((Transaction) Tlb.load(Transaction.class, cs.loadRef()))
                                .build();
                    }
                    case 0b010 -> {
                        return OutMsgImm.builder()
                                .msg((MsgEnvelope) Tlb.load(Message.class, cs.loadRef()))
                                .transaction((Transaction) Tlb.load(Transaction.class, cs.loadRef()))
                                .reimport((InMsg) Tlb.load(InMsg.class, cs.loadRef()))
                                .build();
                    }
                    case 0b001 -> {
                        return OutMsgNew.builder()
                                .outMsg((MsgEnvelope) Tlb.load(Message.class, cs.loadRef()))
                                .transaction((Transaction) Tlb.load(Transaction.class, cs.loadRef()))
                                .build();
                    }
                    case 0b011 -> {
                        return OutMsgTr.builder()
                                .outMsg((MsgEnvelope) Tlb.load(Message.class, cs.loadRef()))
                                .imported((InMsg) Tlb.load(InMsg.class, cs.loadRef()))
                                .build();
                    }
                    case 0b111 -> {
                        return OutMsgTrReq.builder()
                                .msg((MsgEnvelope) Tlb.load(Message.class, cs.loadRef()))
                                .imported((InMsg) Tlb.load(InMsg.class, cs.loadRef()))
                                .build();
                    }
                    case 0b100 -> {
                        return OutMsgDeqImm.builder()
                                .msg((MsgEnvelope) Tlb.load(Message.class, cs.loadRef()))
                                .reimport((InMsg) Tlb.load(InMsg.class, cs.loadRef()))
                                .build();
                    }
                    case 0b110 -> {
                        boolean outMsgSubFlag = cs.loadBit();
                        if (outMsgSubFlag) { // 001101 msg_export_deq_short$1101
                            return OutMsgDeqShort.builder()
                                    .msgEnvHash(cs.loadUint(256))
                                    .nextWorkchain(cs.loadUint(32))
                                    .nextAddrPfx(cs.loadUint(64))
                                    .importBlockLt(cs.loadUint(64))
                                    .build();
                        } else {
                            return OutMsgDeq.builder()
                                    .outMsg((MsgEnvelope) Tlb.load(Message.class, cs.loadRef()))
                                    .importBlockLt(cs.loadUint(64))
                                    .build();
                        }
                    }
                }
                throw new Error("unknown out_msg flag, found 0x" + Long.toBinaryString(outMsgFlag));
            }
            case "InMsg": {
                int inMsgFlag = cs.loadUint(3).intValue();
                switch (inMsgFlag) {
                    case 0b000 -> {
                        return InMsgImportExt.builder()
                                .msg((Message) Tlb.load(Message.class, cs.loadRef()))
                                .transaction((Transaction) Tlb.load(Transaction.class, cs.loadRef()))
                                .build();
                    }
                    case 0b010 -> {
                        return InMsgImportIhr.builder()
                                .msg((Message) Tlb.load(Message.class, cs.loadRef()))
                                .transaction((Transaction) Tlb.load(Transaction.class, cs.loadRef()))
                                .ihrFee(cs.loadCoins())
                                .proofCreated(cs.loadRef())
                                .build();
                    }
                    case 0b011 -> {
                        return InMsgImportImm.builder()
                                .inMsg((MsgEnvelope) Tlb.load(MsgEnvelope.class, cs.loadRef()))
                                .transaction((Transaction) Tlb.load(Transaction.class, cs.loadRef()))
                                .fwdFee(cs.loadCoins())
                                .build();
                    }
                    case 0b100 -> {
                        return InMsgImportFin.builder()
                                .inMsg((MsgEnvelope) Tlb.load(MsgEnvelope.class, cs.loadRef()))
                                .transaction((Transaction) Tlb.load(Transaction.class, cs.loadRef()))
                                .fwdFee(cs.loadCoins())
                                .build();
                    }
                    case 0b101 -> {
                        return InMsgImportTr.builder()
                                .inMsg((MsgEnvelope) Tlb.load(MsgEnvelope.class, cs.loadRef()))
                                .outMsg((MsgEnvelope) Tlb.load(MsgEnvelope.class, cs.loadRef()))
                                .transitFee(cs.loadCoins())
                                .build();
                    }
                    case 0b110 -> {
                        return InMsgDiscardFin.builder()
                                .inMsg((MsgEnvelope) Tlb.load(MsgEnvelope.class, cs.loadRef()))
                                .transactionId(cs.loadUint(64))
                                .fwdFee(cs.loadCoins())
                                .build();
                    }
                    case 0b111 -> {
                        return InMsgDiscardTr.builder()
                                .inMsg((MsgEnvelope) Tlb.load(MsgEnvelope.class, cs.loadRef()))
                                .transactionId(cs.loadUint(64))
                                .fwdFee(cs.loadCoins())
                                .proofDelivered(cs.loadRef())
                                .build();
                    }
                }
                throw new Error("unknown in_msg flag, found 0x" + Long.toBinaryString(inMsgFlag));
            }
            case "InMsgDescr":
                return InMsgDescr.builder()
                        .inMsg(cs.loadDictAugE(256,
                                k -> k.readInt(256),
                                v -> CellSlice.beginParse(v).loadTlb(InMsg.class),
                                e -> CellSlice.beginParse(e).loadTlb(ImportFees.class)))
                        .build();
            case "OutMsgDescr":
                return OutMsgDescr.builder()
                        .outMsg(cs.loadDictAugE(256,
                                k -> k.readInt(256),
                                v -> CellSlice.beginParse(v).loadTlb(OutMsg.class),
                                e -> CellSlice.beginParse(e).loadTlb(CurrencyCollection.class)))
                        .build();
            case "LibDescr": {
                magic = cs.loadUint(2).longValue();
                assert (magic == 0b00) : "LibDescr: magic not equal to 0b00, found 0x" + Long.toHexString(magic);
                return LibDescr.builder()
                        .magic(0b00)
                        .lib(cs.loadRef())
                        .publishers(cs.loadDict(256,
                                k -> k.readInt(256),
                                v -> CellSlice.beginParse(v).loadBit()))
                        .build();
            }
            case "ValidatorBaseInfo": {
                return ValidatorBaseInfo.builder()
                        .validatorListHashShort(cs.loadUint(32).longValue())
                        .catchainSeqno(cs.loadUint(32).longValue())
                        .build();
            }
            case "BlockSignaturesPure": {
                return BlockSignaturesPure.builder()
                        .sigCount(cs.loadUint(32).longValue())
                        .sigWeight(cs.loadUint(64))
                        .signatures(cs.loadDictE(16,
                                k -> k.readUint(16),
                                v -> CellSlice.beginParse(v).loadTlb(CryptoSignaturePair.class)))
                        .build();
            }
            case "BlockSignatures": {
                magic = cs.loadUint(2).longValue();
                assert (magic == 0b11) : "BlockSignatures: magic not equal to 0b11, found 0b" + Long.toBinaryString(magic);

                return BlockSignatures.builder()
                        .magic(0b11)
                        .validatorBaseInfo((ValidatorBaseInfo) cs.loadTlb(ValidatorBaseInfo.class))
                        .pureSignatures((BlockSignaturesPure) cs.loadTlb(BlockSignaturesPure.class))
                        .build();
            }
            case "BlockProof": {
                magic = cs.loadUint(8).longValue();
                assert (magic == 0xc3) : "BlockProof: magic not equal to 0xc3, found 0x" + Long.toHexString(magic);

                return BlockProof.builder()
                        .magic(0xc3)
                        .proofFor((BlockIdExt) cs.loadTlb(BlockIdExt.class))
                        .root(cs.loadRef())
                        .signatures(cs.loadBit() ? (BlockSignatures) Tlb.load(BlockSignatures.class, cs.loadRef()) : null)
                        .build();
            }
            case "BlockInfo": {
                magic = cs.loadUint(32).longValue();
                assert (magic == 0x9bc7a987L) : "BlockInfo: magic not equal to 0x9bc7a987, found 0x" + Long.toHexString(magic);

                BlockInfo blockInfo = BlockInfo.builder()
                        .magic(0x9bc7a987L)
                        .version(cs.loadUint(32).longValue())
                        .notMaster(cs.loadBit())
                        .afterMerge(cs.loadBit())
                        .beforeSplit(cs.loadBit())
                        .afterSplit(cs.loadBit())
                        .wantSplit(cs.loadBit())
                        .wantMerge(cs.loadBit())
                        .keyBlock(cs.loadBit())
                        .vertSeqnoIncr(cs.loadBit())
                        .flags(cs.loadUint(8).longValue())
                        .seqno(cs.loadUint(32).longValue())
                        .vertSeqno(cs.loadUint(32).longValue())
                        .shard((ShardIdent) cs.loadTlb(ShardIdent.class))
                        .genuTime(cs.loadUint(32).longValue())
                        .startLt(cs.loadUint(64))
                        .endLt(cs.loadUint(64))
                        .genValidatorListHashShort(cs.loadUint(32).longValue())
                        .genCatchainSeqno(cs.loadUint(32).longValue())
                        .minRefMcSeqno(cs.loadUint(32).longValue())
                        .prevKeyBlockSeqno(cs.loadUint(32).longValue())
                        .build();
                blockInfo.setGlobalVersion(((blockInfo.getFlags() & 0x1L) == 0x1L) ? (GlobalVersion) cs.loadTlb(GlobalVersion.class) : null);
                blockInfo.setMasterRef(blockInfo.isNotMaster() ? (ExtBlkRef) Tlb.load(ExtBlkRef.class, cs.loadRef()) : null);
                blockInfo.setPrefRef(loadBlkPrevInfo(CellSlice.beginParse(cs.loadRef()), blockInfo.isAfterMerge()));
                blockInfo.setPrefVertRef(blockInfo.isVertSeqnoIncr() ? loadBlkPrevInfo(CellSlice.beginParse(cs.loadRef()), blockInfo.isAfterMerge()) : null);
                return blockInfo;
            }
            case "BlockHandle":
                return BlockHandle.builder()
                        .offset(cs.loadUint(64))
                        .size(cs.loadUint(64))
                        .build();
            case "BlockExtra":
                magic = cs.loadUint(32).longValue();
                assert (magic == 0x4a33f6fdL) : "Block: magic not equal to 0x4a33f6fdL, found 0x" + Long.toHexString(magic);

                BlockExtra blockExtra = BlockExtra.builder()
                        .inMsgDesc((InMsgDescr) Tlb.load(InMsgDescr.class, cs.loadRef()))
                        .outMsgDesc((OutMsgDescr) Tlb.load(OutMsgDescr.class, cs.loadRef()))
                        .shardAccountBlocks(CellSlice.beginParse(cs.loadRef()).loadDictAugE(256, // todo review from ref
                                k -> k.readUint(256),
                                v -> v.loadTlb(AccountBlock.class),
                                e -> e.loadTlb(CurrencyCollection.class)))
                        .randSeed(cs.loadUint(256))
                        .createdBy(cs.loadUint(256))
                        // was commented out
                        .custom(isNull(cs.preloadMaybeRefX()) ? null : (McBlockExtra) Tlb.load(McBlockExtra.class, CellSlice.beginParse(cs.loadRef())))
                        .build();
                return blockExtra;
            case "Block":
                System.out.println("block " + cs.sliceToCell().toHex());

                magic = cs.loadUint(32).longValue();
                assert (magic == 0x11ef55aaL) : "Block: magic not equal to 0x11ef55aa, found 0x" + Long.toHexString(magic);

                return Block.builder()
                        .magic(0x11ef55aaL)
                        .globalId(cs.loadInt(32).intValue())
                        .blockInfo((BlockInfo) Tlb.load(BlockInfo.class, cs.loadRef()))
                        .valueFlow((ValueFlow) Tlb.load(ValueFlow.class, cs.loadRef()))
                        .stateUpdate((StateUpdate) Tlb.load(StateUpdate.class, cs.loadRef()))
                        .extra((BlockExtra) Tlb.load(BlockExtra.class, cs.loadRef()))//  todo test testLoadBlockNotMaster
                        .build();
            case "AccountBlock":
                magic = cs.loadUint(4).longValue();
                assert (magic == 0x5L) : "AccountBlock: magic not equal to 0x5, found 0x" + Long.toHexString(magic);

                return AccountBlock.builder()
                        .magic(0x5)
                        .addr(cs.loadUint(256))
                        .transactions(cs.loadDictAugE(64,
                                k -> k.readInt(64),
                                v -> Tlb.load(Transaction.class, v.loadRef()),
                                e -> e.loadTlb(CurrencyCollection.class)))
                        .stateUpdate(cs.loadRef())
                        .build();
            case "ValueFlow":
                magic = cs.loadUint(32).longValue();
                assert (magic == 0xb8e48dfbL) : "ValueFlow: magic not equal to 0xb8e48dfb, found 0x" + Long.toHexString(magic);

                Cell c1 = cs.loadRef();
                Cell c2 = cs.loadRef();
                CurrencyCollection fromPrevBlk = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c1);
                CurrencyCollection toNextBlk = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c1);
                CurrencyCollection imported = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c1);
                CurrencyCollection exported = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c1);
                CurrencyCollection feesCollected = (CurrencyCollection) Tlb.load(CurrencyCollection.class, cs);
                CurrencyCollection feesImported = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c2);
                CurrencyCollection recovered = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c2);
                CurrencyCollection created = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c2);
                CurrencyCollection minted = (CurrencyCollection) Tlb.load(CurrencyCollection.class, c2);

                return ValueFlow.builder()
                        .magic(0xb8e48dfbL)
                        .fromPrevBlk(fromPrevBlk)
                        .toNextBlk(toNextBlk)
                        .imported(imported)
                        .exported(exported)
                        .feesCollected(feesCollected)
                        .feesImported(feesImported)
                        .recovered(recovered)
                        .created(created)
                        .minted(minted)
                        .build();
            case "ValidatorSet":
                return ValidatorSet.builder().build();
            case "ValidatorSetExt":
                return ValidatorSetExt.builder().build();
            case "Validator":
                return Validator.builder().build();
            case "ValidatorAddr":
                return ValidatorAddr.builder().build();
            case "SigPubKeyED25519":
                return SigPubKeyED25519.builder().build();
            case "Anycast":
                int depth = cs.loadUint(5).intValue();
                return Anycast.builder()
                        .depth(depth)
                        .rewritePfx(cs.loadUint(depth).byteValueExact())
                        .build();
            case "MsgAddressInt": {
                MsgAddressInt intMsgAddr = null;
                int flagMsg = cs.loadUint(2).intValue();
                switch (flagMsg) {
                    case 0b10 -> {
                        intMsgAddr = (MsgAddressInt) cs.loadTlb(MsgAddressIntStd.class);
                    }
                    case 0b11 -> {
                        intMsgAddr = (MsgAddressInt) cs.loadTlb(MsgAddressIntVar.class);
                    }
                }
                return intMsgAddr;
            }
            case "MsgAddressIntStd": {
                Anycast anycast = null;
                if (cs.loadBit()) {
                    anycast = (Anycast) cs.loadTlb(Anycast.class);
                }
                return MsgAddressIntStd.builder()
                        .magic(0b10)
                        .anycast(anycast)
                        .workchainId(cs.loadUint(8).byteValue())
                        .address(cs.loadUint(256))
                        .build();
            }
            case "MsgAddressIntVar": {
                Anycast anycast = null;
                if (cs.loadBit()) {
                    anycast = (Anycast) cs.loadTlb(Anycast.class);
                }
                int addrLen = cs.loadUint(9).intValue();
                return MsgAddressIntVar.builder()
                        .magic(0b11)
                        .anycast(anycast)
                        .addrLen(addrLen)
                        .workchainId(cs.loadUint(32).intValue())
                        .address(cs.loadUint(addrLen))
                        .build();
            }
            case "MsgAddress": {
                MsgAddressExt extMsgAddr = null;
                MsgAddressInt intMsgAddr = null;
                int flagMsg = cs.loadUint(2).intValue();
                switch (flagMsg) {
                    case 0b00 -> {
                        extMsgAddr = MsgAddressExt.builder().build();
                    }
                    case 0b01 -> {
                        int len = cs.loadUint(9).intValue();
                        BigInteger externalAddress = cs.loadUint(len);
                        extMsgAddr = MsgAddressExt.builder()
                                .len(len)
                                .externalAddress(externalAddress)
                                .build();
                    }
                    case 0b10 -> {
                        intMsgAddr = (MsgAddressInt) cs.loadTlb(MsgAddressIntStd.class);
                    }
                    case 0b11 -> {
                        intMsgAddr = (MsgAddressInt) cs.loadTlb(MsgAddressIntVar.class);
                    }
                }
                return MsgAddress.builder()
                        .magic(flagMsg)
                        .msgAddressExt(extMsgAddr)
                        .msgAddressInt(intMsgAddr)
                        .build();
            }
            case "Message": {
                CommonMsgInfo commonMsgInfo = (CommonMsgInfo) cs.loadTlb(CommonMsgInfo.class);
                return Message.builder()
                        .info(commonMsgInfo)
                        .init(cs.loadBit() ? (cs.loadBit() ? (StateInit) Tlb.load(StateInit.class, cs.loadRef()) : (StateInit) cs.loadTlb(StateInit.class)) : null) //review
                        .body(cs.loadBit() ? cs.loadRef() : CellBuilder.beginCell().storeBitString(cs.loadBits(cs.getRestBits()))) // todo
                        .build();
            }
            case "CommonMsgInfo": {
                if (isNull(cs)) {
                    return Message.builder().build();
                }
                boolean isExternal = cs.preloadBit();
                if (!isExternal) {
                    InternalMessage internalMessage = (InternalMessage) cs.loadTlb(InternalMessage.class);

                    return CommonMsgInfo.builder()
                            .msgType("INTERNAL")
                            .msg(internalMessage)
                            .build();

//                    return Message.builder()
//                            .msgType("INTERNAL")
//                            .msg(AnyMessage.builder()
//                                    .payload(internalMessage.getBody())
////                                    .destAddr(internalMessage.getDstAddr()) //todo
////                                    .senderAddr(internalMessage.getSrcAddr())
//                                    .build())
//                            .build();
                } else {
                    boolean isOut = cs.preloadBitAt(2);
                    if (isOut) {
                        ExternalMessageOut externalMessageOut = (ExternalMessageOut) cs.loadTlb(ExternalMessageOut.class);
                        return CommonMsgInfo.builder()
                                .msgType("EXTERNAL_OUT")
                                .msg(externalMessageOut)
                                .build();
//                        return Message.builder()
//                                .msgType("EXTERNAL_OUT")
//                                .msg(AnyMessage.builder()
//                                        .payload(externalMessageOut.getBody())
////                                        .destAddr(externalMessageOut.getDstAddr())
////                                        .senderAddr(externalMessageOut.getSrcAddr())
//                                        .build())
//                                .build();
                    } else {
                        ExternalMessage externalMessage = (ExternalMessage) cs.loadTlb(ExternalMessage.class);
                        return CommonMsgInfo.builder()
                                .msgType("EXTERNAL_IN")
                                .msg(externalMessage)
                                .build();
//                        return Message.builder()
//                                .msgType("EXTERNAL_IN")
//                                .msg(AnyMessage.builder()
//                                        .payload(externalMessage.getBody())
////                                        .destAddr(externalMessage.getDstAddr())
////                                        .senderAddr(externalMessage.getSrcAddr())
//                                        .build())
//                                .build();
                    }
                }
                //throw new Error("Unknown msg type ");
            }
            case "MessagesList":
                if (isNull(cs)) {
                    return MessagesList.builder().build();
                }
                return MessagesList.builder()
                        .list(cs.loadDictE(15,
                                k -> k.readInt(15),
                                v -> CellSlice.beginParse(v).loadTlb(Message.class)))
                        .build();
            case "ComputeSkipReason": {
                int skipReasonFlag = cs.loadUint(2).intValue();

                switch (skipReasonFlag) {
                    case 0b00 -> {
                        return ComputeSkipReason.builder().type("NO_STATE").build();
                    }
                    case 0b01 -> {
                        return ComputeSkipReason.builder().type("BAD_STATE").build();
                    }
                    case 0b10 -> {
                        return ComputeSkipReason.builder().type("NO_GAS").build();
                    }
                    case 0b11 -> {
                        boolean isNotSuspended = cs.loadBit();
                        if (!isNotSuspended) {
                            return ComputeSkipReason.builder().type("SUSPENDED").build();
                        }
                    }
                }
                throw new Error("unknown compute skip reason, found 0x" + Integer.toBinaryString(skipReasonFlag));
            }
            case "ComputePhase": {
                boolean isNotSkipped = cs.loadBit();
                if (isNotSkipped) {
                    return cs.loadTlb(ComputePhaseVM.class);
                }
                return cs.loadTlb(ComputeSkipReason.class);
            }
            case "BouncePhaseNegFounds": {
                magic = cs.loadUint(1).intValue(); // review, should be 2
                assert (magic == 0b0) : "BouncePhaseNegFounds: magic not equal to 0b0, found 0x" + Long.toHexString(magic);

                return BouncePhaseNegFounds.builder().build();
            }
            case "BouncePhaseNoFounds": {
                magic = cs.loadUint(2).intValue();
                assert (magic == 0b01) : "BouncePhaseNoFounds: magic not equal to 0b01, found 0x" + Long.toHexString(magic);

                return BouncePhaseNoFounds.builder()
                        .msgSize((StorageUsedShort) cs.loadTlb(StorageUsedShort.class))
                        .reqFwdFees(cs.loadCoins())
                        .build();
            }
            case "BouncePhaseok": {
                magic = cs.loadUint(1).intValue();
                assert (magic == 0b1) : "BouncePhaseok: magic not equal to 0b1, found 0x" + Long.toHexString(magic);

                return BouncePhaseok.builder()
                        .magic(0b1)
                        .msgSize((StorageUsedShort) cs.loadTlb(StorageUsedShort.class))
                        .msgFees(cs.loadCoins())
                        .fwdFees(cs.loadCoins())
                        .build();
            }
            case "BouncePhase": {
                boolean isOk = cs.preloadBit();
                if (isOk) {
                    return cs.loadTlb(BouncePhaseok.class); // skipped was true
                }
                //cs.loadBit();
                boolean isNoFunds = cs.preloadBit();
                if (isNoFunds) {
                    return cs.loadTlb(BouncePhaseNoFounds.class); // skipped was true
                }
                return cs.loadTlb(BouncePhaseNegFounds.class); // skipped was true
            }
            case "StoragePhase": {
                return StoragePhase.builder()
                        .storageFeesCollected(cs.loadCoins())
                        .storageFeesDue(cs.loadBit() ? cs.loadCoins() : null)
                        .statusChange((AccStatusChange) cs.loadTlb(AccStatusChange.class))
                        .build();
            }
            case "ActionPhase": {
                return ActionPhase.builder()
                        .success(cs.loadBit())
                        .valid(cs.loadBit())
                        .noFunds(cs.loadBit())
                        .statusChange((AccStatusChange) cs.loadTlb(AccStatusChange.class))
                        .totalFwdFees(cs.loadBit() ? cs.loadCoins() : null)
                        .totalActionFees(cs.loadBit() ? cs.loadCoins() : null)
                        .resultCode(cs.loadUint(32).longValue())
                        .resultArg(cs.loadBit() ? cs.loadUint(32).longValue() : 0)
                        .totalActions(cs.loadUint(16).longValue())
                        .specActions(cs.loadUint(16).longValue())
                        .skippedActions(cs.loadUint(16).longValue())
                        .messagesCreated(cs.loadUint(16).longValue())
                        .actionListHash(cs.loadUint(256))
                        .totalMsgSize((StorageUsedShort) cs.loadTlb(StorageUsedShort.class))
                        .build();
            }

            case "StorageUsedShort": {
                return StorageUsedShort.builder()
                        .cells(cs.loadVarUInteger(BigInteger.valueOf(7)))
                        .bits(cs.loadVarUInteger(BigInteger.valueOf(7)))
                        .build();
            }
            case "ComputePhaseVM": {
                return ComputePhaseVM.builder()
                        .magic(1)
                        .success(cs.loadBit())
                        .msgStateUsed(cs.loadBit())
                        .accountActivated(cs.loadBit())
                        .gasFees(cs.loadCoins())
                        .details((ComputePhaseVMDetails) Tlb.load(ComputePhaseVMDetails.class, cs.loadRef()))
                        .build();
            }
            case "ComputePhaseVMDetails": {
                return ComputePhaseVMDetails.builder()
                        .gasUsed(cs.loadVarUInteger(BigInteger.valueOf(7)))
                        .gasLimit(cs.loadVarUInteger(BigInteger.valueOf(7)))
                        .gasCredit(cs.loadBit() ? cs.loadVarUInteger(BigInteger.valueOf(3)) : BigInteger.ZERO)
                        .mode(cs.loadUint(8).intValue())
                        .exitCode(cs.loadUint(32).longValue())
                        .exitArg(cs.loadBit() ? cs.loadUint(32).longValue() : 0L)
                        .vMSteps(cs.loadUint(32).longValue())
                        .vMInitStateHash(cs.loadUint(256))
                        .vMFinalStateHash(cs.loadUint(256))
                        .build();
            }
            case "CreditPhase": {
                return CreditPhase.builder()
                        .dueFeesCollected(cs.loadBit() ? cs.loadCoins() : BigInteger.ZERO)
                        .credit((CurrencyCollection) cs.loadTlb(CurrencyCollection.class))
                        .build();
            }
            case "SplitMergeInfo": {
                return SplitMergeInfo.builder()
                        .curShardPfxLen(cs.loadUint(6).intValue())
                        .accSplitDepth(cs.loadUint(6).intValue())
                        .thisAddr(cs.loadUint(256))
                        .siblingAddr(cs.loadUint(256))
                        .build();
            }
            case "Transaction": {
                magic = cs.loadUint(4).intValue();
                assert (magic == 0b0111) : "Transaction: magic not equal to 0b0111, found 0b" + Long.toBinaryString(magic);

                Transaction tx = Transaction.builder()
                        .magic(0b0111)
                        .accountAddr(cs.loadUint(256))
                        .lt(cs.loadUint(64))
                        .prevTxHash(cs.loadUint(256))
                        .prevTxLt(cs.loadUint(64))
                        .now(cs.loadUint(32).longValue())
                        .outMsgCount(cs.loadUint(15).intValue())
                        .origStatus(deserializeAccountState(cs.loadUint(2).byteValue()))
                        .endStatus(deserializeAccountState(cs.loadUint(2).byteValueExact()))
                        .totalFees((CurrencyCollection) cs.loadTlb(CurrencyCollection.class))
                        .build();

                CellSlice io = CellSlice.beginParse(cs.loadRef());
                Message msg = (Message) Tlb.load(Message.class, io.loadMaybeRefX());
                Cell outMsgDict = io.loadMaybeRefX();
                TonHashMapE out = nonNull(outMsgDict) ? CellSlice.beginParse(outMsgDict).loadDictE(15,
                        k -> k.readInt(15),
                        v -> CellSlice.beginParse(v).loadTlb(Message.class)) : null;

                tx.setInOut(TransactionIO.builder()
                        .in(msg)
                        .out(out)
                        .build());

                if (nonNull(tx.getInOut().getOut())) { // todo cleanup
                    for (Map.Entry<Object, Object> entry : tx.getInOut().getOut().elements.entrySet()) {
                        System.out.println("key " + entry.getKey() + ", value " + ((Cell) entry.getValue()).print());
                        Message i = (Message) Tlb.load(Message.class, (Cell) entry.getValue());
                        System.out.println("i = " + i.toString());
                    }
                }

//                tx.setTotalFees((CurrencyCollection) cs.loadTlb(CurrencyCollection.class));
                tx.setStateUpdate((HashUpdate) Tlb.load(HashUpdate.class, cs.loadRef()));
                tx.setDescription((TransactionDescription) Tlb.load(TransactionDescription.class, cs.loadRef()));

                return tx;
            }
            case "HashUpdate": {
                magic = cs.loadUint(8).intValue();
                assert (magic == 0x72) : "HashUpdate: magic not equal to 0x72, found 0x" + Long.toHexString(magic);

                return HashUpdate.builder()
                        .magic(0x72)
                        .oldHash(cs.loadUint(256))
                        .newHash(cs.loadUint(256))
                        .build();
            }
            case "TransactionDescription": {
                int pfx = cs.preloadUint(3).intValue();
                switch (pfx) {
                    case 0b000 -> {
                        boolean isStorage = cs.preloadBit();
                        if (isStorage) {
                            TransactionDescriptionStorage desc = (TransactionDescriptionStorage) cs.loadTlb(TransactionDescriptionStorage.class); // skipped was true
                            return TransactionDescription.builder().description(desc).build();
                        }
                        TransactionDescriptionOrdinary descOrdinary = (TransactionDescriptionOrdinary) cs.loadTlb(TransactionDescriptionOrdinary.class); // skipped was true
                        return TransactionDescription.builder().description(descOrdinary).build();
                    }
                    case 0b001 -> {
                        TransactionDescriptionTickTock descTickTock = (TransactionDescriptionTickTock) cs.loadTlb(TransactionDescriptionTickTock.class); // skipped was true
                        return TransactionDescription.builder().description(descTickTock).build();
                    }
                    case 0b010 -> {
                        boolean isInstall = cs.preloadBit();
                        if (isInstall) {
                            TransactionDescriptionSplitInstall descSplit = (TransactionDescriptionSplitInstall) cs.loadTlb(TransactionDescriptionSplitInstall.class); // skipped was true
                            return TransactionDescription.builder().description(descSplit).build();
                        }
                        TransactionDescriptionSplitPrepare descSplitPrepare = (TransactionDescriptionSplitPrepare) cs.loadTlb(TransactionDescriptionSplitPrepare.class); // skipped was true
                        return TransactionDescription.builder().description(descSplitPrepare).build();
                    }
                    case 0b011 -> {
                        boolean isInstall = cs.preloadBit();
                        if (isInstall) {
                            TransactionDescriptionMergeInstall descMerge = (TransactionDescriptionMergeInstall) cs.loadTlb(TransactionDescriptionMergeInstall.class); // skipped was true
                            return TransactionDescription.builder().description(descMerge).build();
                        }
                        TransactionDescriptionMergePrepare descMergePrepare = (TransactionDescriptionMergePrepare) cs.loadTlb(TransactionDescriptionMergePrepare.class); // skipped was true
                        return TransactionDescription.builder().description(descMergePrepare).build();
                    }
                }
                throw new Error("unknown transaction description type (must be in range [0..3], found 0x" + Integer.toBinaryString(pfx));
            }
            case "TransactionDescriptionStorage": {
                magic = cs.loadUint(4).intValue();
                assert (magic == 0b0001) : "TransactionDescriptionStorage: magic not equal to 0b0001, found 0x" + Long.toHexString(magic);

                return TransactionDescriptionStorage.builder()
                        .magic(0b0001)
                        .storagePhase((StoragePhase) Tlb.load(StoragePhase.class, cs))
                        .build();
            }
            case "TransactionDescriptionOrdinary": {
                magic = cs.loadUint(4).intValue();
                assert (magic == 0b0000) : "TransactionDescriptionOrdinary: magic not equal to 0b0000, found 0x" + Long.toHexString(magic);

                return TransactionDescriptionOrdinary.builder()
                        .magic(0b0000)
                        .creditFirst(cs.loadBit())
                        .storagePhase(cs.loadBit() ? (StoragePhase) cs.loadTlb(StoragePhase.class) : null)
                        .creditPhase(cs.loadBit() ? (CreditPhase) cs.loadTlb(CreditPhase.class) : null)
                        .computePhase((ComputePhase) cs.loadTlb(ComputePhase.class))
                        .actionPhase((ActionPhase) Tlb.load(ActionPhase.class, cs.loadMaybeRefX()))
                        .aborted(cs.loadBit())
                        .bouncePhase(cs.loadBit() ? (BouncePhase) cs.loadTlb(BouncePhase.class) : null)
                        .destroyed(cs.loadBit())
                        .build();


            }
            case "TransactionDescriptionTickTock": {
                magic = cs.loadUint(3).intValue();
                assert (magic == 0b001) : "TransactionDescriptionTickTock: magic not equal to 0b001, found 0x" + Long.toHexString(magic);

                return TransactionDescriptionTickTock.builder()
                        .magic(0b001)
                        .isTock(cs.loadBit())
                        .storagePhase((StoragePhase) cs.loadTlb(StoragePhase.class))
                        .computePhase((ComputePhase) cs.loadTlb(ComputePhase.class))
                        .actionPhase((ActionPhase) Tlb.load(ActionPhase.class, cs.loadMaybeRefX()))
                        .aborted(cs.loadBit())
                        .destroyed(cs.loadBit())
                        .build();
            }
            case "TransactionDescriptionSplitInstall": {
                magic = cs.loadUint(4).intValue();
                assert (magic == 0b0101) : "TransactionDescriptionSplitInstall: magic not equal to 0b0101, found 0x" + Long.toHexString(magic);

                return TransactionDescriptionSplitInstall.builder()
                        .magic(0b0101)
                        .splitInfo((SplitMergeInfo) cs.loadTlb(SplitMergeInfo.class))
                        .prepareTransaction((Transaction) Tlb.load(Transaction.class, cs.loadRef()))
                        .installed(cs.loadBit())
                        .build();
            }
            case "TransactionDescriptionSplitPrepare": {
                magic = cs.loadUint(4).intValue();
                assert (magic == 0b0100) : "TransactionDescriptionSplitPrepare: magic not equal to 0b0100, found 0x" + Long.toHexString(magic);

                return TransactionDescriptionMergeInstall.builder()
                        .magic(0b0100)
                        .splitInfo((SplitMergeInfo) cs.loadTlb(SplitMergeInfo.class))
                        .storagePhase(cs.loadBit() ? (StoragePhase) cs.loadTlb(StoragePhase.class) : null)
                        .computePhase((ComputePhase) cs.loadTlb(ComputePhase.class))
                        .actionPhase((ActionPhase) Tlb.load(ActionPhase.class, cs.loadMaybeRefX()))
                        .aborted(cs.loadBit())
                        .destroyed(cs.loadBit())
                        .build();
            }
            case "TransactionDescriptionMergeInstall": {
                magic = cs.loadUint(4).intValue();
                assert (magic == 0b0111) : "TransactionDescriptionMergeInstall: magic not equal to 0b0111, found 0x" + Long.toHexString(magic);

                return TransactionDescriptionMergeInstall.builder()
                        .magic(0b0111)
                        .splitInfo((SplitMergeInfo) cs.loadTlb(SplitMergeInfo.class))
                        .prepareTransaction((Transaction) Tlb.load(Transaction.class, cs.loadRef()))
                        .storagePhase(cs.loadBit() ? (StoragePhase) cs.loadTlb(StoragePhase.class) : null)
                        .creditPhase(cs.loadBit() ? (CreditPhase) cs.loadTlb(CreditPhase.class) : null)
                        .computePhase((ComputePhase) cs.loadTlb(ComputePhase.class))
                        .actionPhase((ActionPhase) Tlb.load(ActionPhase.class, cs.loadMaybeRefX()))
                        .aborted(cs.loadBit())
                        .destroyed(cs.loadBit())
                        .build();
            }
            case "TransactionDescriptionMergePrepare": {
                magic = cs.loadUint(4).intValue();
                assert (magic == 0b0110) : "TransactionDescriptionMergePrepare: magic not equal to 0b0110, found 0x" + Long.toHexString(magic);

                return TransactionDescriptionMergePrepare.builder()
                        .magic(0b0110)
                        .splitInfo((SplitMergeInfo) cs.loadTlb(SplitMergeInfo.class))
                        .storagePhase((StoragePhase) cs.loadTlb(StoragePhase.class))
                        .aborted(cs.loadBit())
                        .build();
            }

            case "AccStatusChange":
                boolean isChanged = cs.loadBit();
                if (isChanged) {
                    boolean isDeleted = cs.loadBit();
                    if (isDeleted) {
                        return AccStatusChange.builder().type("DELETED").build();
                    }
                    return AccStatusChange.builder().type("FROZEN").build();
                }
                return AccStatusChange.builder().type("UNCHANGED").build();
            case "InternalMessage":
                boolean magicBool = cs.loadBit();
                assert (!magicBool) : "InternalMessage: magic not equal to 0, found 0x" + magicBool;

                return InternalMessage.builder()
                        .magic(0L)
                        .iHRDisabled(cs.loadBit())
                        .bounce(cs.loadBit())
                        .bounced(cs.loadBit())
                        .srcAddr((MsgAddress) cs.loadTlb(MsgAddress.class))
                        .dstAddr((MsgAddress) cs.loadTlb(MsgAddress.class))
                        .value((CurrencyCollection) cs.loadTlb(CurrencyCollection.class))
                        .iHRFee(cs.loadCoins())
                        .fwdFee(cs.loadCoins())
                        .createdLt(cs.loadUint(64))
                        .createdAt(cs.loadUint(32).longValue())
                        .build();
            case "ExternalMessage":
                magic = cs.loadUint(2).intValue();
                assert (magic == 0b10) : "ExternalMessage: magic not equal to 0b10, found 0b" + Long.toBinaryString(magic);
                return ExternalMessage.builder()
                        .magic(2L)
                        .srcAddr((MsgAddress) cs.loadTlb(MsgAddress.class))
                        .dstAddr((MsgAddress) cs.loadTlb(MsgAddress.class))
                        .importFee(cs.loadCoins())
                        .build();
            case "ExternalMessageOut":
                magic = cs.loadUint(2).intValue();
                assert (magic == 0b11) : "ExternalMessageOut: magic not equal to 0b11, found 0b" + Long.toBinaryString(magic);
                return ExternalMessageOut.builder()
                        .magic(3L)
                        .srcAddr((MsgAddress) cs.loadTlb(MsgAddress.class))
                        .dstAddr((MsgAddress) cs.loadTlb(MsgAddress.class))
                        .createdLt(cs.loadUint(64))
                        .createdAt(cs.loadUint(32).longValue())
                        .build();
            case "Text":
                int chunksNum = cs.loadUint(8).intValue();
                int firstSize = 0;
                int lengthOfChunk = 0;
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < chunksNum; i++) {
                    lengthOfChunk = cs.loadUint(8).intValue();
                    if (i == 0) {
                        firstSize = lengthOfChunk;
                    }
                    int[] dataOfChunk = cs.loadBytes(lengthOfChunk * 8);
                    result.append(new String(Utils.unsignedBytesToSigned(dataOfChunk)));

                    if (i < chunksNum - 1) {
                        cs = CellSlice.beginParse(cs.loadRef());
                    }
                }
                return Text.builder()
                        .maxFirstChunkSize(firstSize)
                        .value(result.toString())
                        .build();
        }

        throw new Error("Unknown TLB type: " + c.getSimpleName());
    }

    private static BlkPrevInfo loadBlkPrevInfo(CellSlice cs, boolean afterMerge) {
        BlkPrevInfo blkPrevInfo = BlkPrevInfo.builder().build();
        if (!afterMerge) {
            ExtBlkRef blkRef = (ExtBlkRef) cs.loadTlb(ExtBlkRef.class);
            blkPrevInfo.setPrev1(blkRef);
            return blkPrevInfo;
        }

        ExtBlkRef blkRef1 = (ExtBlkRef) Tlb.load(ExtBlkRef.class, cs.loadRef());
        ExtBlkRef blkRef2 = (ExtBlkRef) Tlb.load(ExtBlkRef.class, cs.loadRef());
        blkPrevInfo.setPrev1(blkRef1);
        blkPrevInfo.setPrev2(blkRef2);
        return blkPrevInfo;
    }

    public static Cell save(Object o) {
        if (o instanceof InMsgImportImm msgImportImm) {
            return msgImportImm.toCell();
        } else if (o instanceof MsgEnvelope msgEnvelope) {
            return msgEnvelope.toCell();
        } else if (o instanceof IntermediateAddressRegular intermediateAddressRegular) {
            return intermediateAddressRegular.toCell();
        } else if (o instanceof IntermediateAddressSimple intermediateAddressRegular) {
            return intermediateAddressRegular.toCell();
        } else if (o instanceof IntermediateAddressExt intermediateAddressExt) {
            return intermediateAddressExt.toCell();
        } else if (o instanceof Message message) {
            return message.toCell();
        } else if (o instanceof Transaction transaction) {
            return transaction.toCell();
        }
        return null;
    }
}
