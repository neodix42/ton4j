package org.ton.java.tlb.loader;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.Map;

import static java.util.Objects.isNull;

public class Tlb {

    public static Object load(Class clazz, Cell c) {
        if (isNull(c)) {
            return null;
        } else {
            return load(clazz, CellSlice.beginParse(c), false);
        }
    }

    public static Object load(Class clazz, Cell c, boolean skipMagic) {
        if (isNull(c)) {
            return null;
        } else {
            return load(clazz, CellSlice.beginParse(c), skipMagic);
        }
    }

    public static Object load(Class c, CellSlice cs) {
        return load(c, cs, false);
    }

    public static Object load(Class c, CellSlice cs, boolean skipMagic) {

        switch (c.getSimpleName()) {
            case "BlockIdExt": // todo
                return BlockIdExt.builder().build();
            case "TickTock":
                return TickTock.builder()
                        .tick(cs.loadBit())
                        .tock(cs.loadBit())
                        .build();
            case "StateInit":
                return StateInit.builder()
                        .depth(cs.loadBit() ? cs.loadUint(5) : BigInteger.ZERO)
                        .tickTock(cs.loadBit() ? (TickTock) Tlb.load(TickTock.class, cs) : null)
                        .code(cs.loadMaybeRefX())
                        .data(cs.loadMaybeRefX())
                        .lib(cs.loadDictE(256, k -> k.readInt(256), v -> v))
                        .build();
            case "AccountStorage":
                AccountStorage accountStorage = AccountStorage.builder().build();

                BigInteger lastTransaction = cs.loadUint(64);
                BigInteger coins = cs.loadCoins();

                boolean extraExists = cs.loadBit();

                if (extraExists) {
                    throw new Error("extra currency info is not supported for AccountStorage");
                }

                boolean isStatusActive = cs.loadBit();
                if (isStatusActive) {
                    accountStorage.setAccountStatus("ACTIVE");
                    StateInit stateInit = (StateInit) Tlb.load(StateInit.class, cs);
                    accountStorage.setStateInit(stateInit);
                } else {
                    boolean isStatusFrozen = cs.loadBit();
                    if (isStatusFrozen) {
                        accountStorage.setAccountStatus("FROZEN");
                        if (cs.getRestBits() != 0) {
                            int[] stateHash = cs.loadBytes(256);
                            accountStorage.setStateHash(stateHash);
                        }
                    } else {
                        accountStorage.setAccountStatus("UNINIT");
                    }
                }
                accountStorage.setLastTransactionLt(lastTransaction);
                accountStorage.setBalance(coins);
                return accountStorage;
            case "StorageInfo":
                StorageUsed storageUsed = (StorageUsed) Tlb.load(StorageUsed.class, cs);
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
            case "AccountState": {
                boolean isAccount = cs.loadBit();
                if (!isAccount) {
                    return null;
                }
                Address address = cs.loadAddress();
                StorageInfo info = (StorageInfo) Tlb.load(StorageInfo.class, cs);
                AccountStorage storage = (AccountStorage) Tlb.load(AccountStorage.class, cs);

                return AccountState.builder()
                        .isValid(true)
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
                return StateUpdate.builder()
                        .oldOne((ShardState) Tlb.load(ShardState.class, CellSlice.beginParse(cs.loadRef())))
                        .newOne(cs.loadRef())
                        .build();
            case "ConfigParams":
                return ConfigParams.builder()
                        .configAddr(Address.of((byte) 0x11, 255, cs.loadBits(256).toByteArray()))
                        .config(CellSlice.beginParse(cs.loadRef()).loadDict(32, k -> k.readUint(32), v -> v))
                        .build();
            case "ShardStateUnsplit":
                return ShardStateUnsplit.builder()
                        .magic(cs.loadUint(32).longValue())
                        .globalId(cs.loadUint(32).intValue())
                        .shardIdent((ShardIdent) Tlb.load(ShardIdent.class, cs))
                        .seqno(cs.loadUint(32).longValue())
                        .vertSeqno(cs.loadUint(32).longValue())
                        .genUTime(cs.loadUint(32).longValue())
                        .genLT(cs.loadUint(64))
                        .minRefMCSeqno(cs.loadUint(32).longValue())
                        .outMsgQueueInfo(cs.loadRef())
                        .beforeSplit(cs.loadBit())
                        .accounts(CellSlice.beginParse(cs.loadRef()).loadDictE(256, k -> k.readInt(256), v -> v))
                        .stats(cs.loadRef())
                        .mc(isNull(cs.preloadMaybeRefX()) ? null : (McStateExtra) Tlb.load(McStateExtra.class, CellSlice.beginParse(cs.loadRef())))
                        .build();
            case "ShardIdent":
                assert skipMagic || (cs.loadUint(2).longValue() == 0L) : "ShardIdent: magic not equal to 0x0";
                return ShardIdent.builder()
                        .magic(0L)
                        .prefixBits(cs.loadUint(6).byteValueExact())
                        .workchain(cs.loadUint(32).longValue())
                        .shardPrefix(cs.loadUint(64))
                        .build();
            case "ShardDesc": // todo
                return ShardDesc.builder().build();
            case "ShardState":
                long tag = cs.preloadUint(32).longValue();
                if (tag == 0x5f327da5L) {
                    ShardStateUnsplit left, right;
                    left = (ShardStateUnsplit) Tlb.load(ShardStateUnsplit.class, CellSlice.beginParse(cs.loadRef()));
                    right = (ShardStateUnsplit) Tlb.load(ShardStateUnsplit.class, CellSlice.beginParse(cs.loadRef()));
                    return ShardState.builder()
                            .left(left)
                            .right(right)
                            .build();
                } else if (tag == 0x9023afe2L) {
                    return ShardState.builder()
                            .left((ShardStateUnsplit) Tlb.load(ShardStateUnsplit.class, cs))
                            .build();
                } else {
                    throw new Error("unknown shardstate tag");
                }
            case "McStateExtra":
                if (isNull(cs)) {
                    return null;
                }
                assert skipMagic || (cs.loadUint(16).longValue() == 0xcc26L) : "McStateExtra: magic not equal to 0xcc26";
                return McStateExtra.builder()
                        .magic(0xcc26L)
                        .shardHashes(cs.loadDictE(32, k -> k.readInt(32), v -> v))
                        .configParams((ConfigParams) Tlb.load(ConfigParams.class, cs))
                        .info(cs.loadRef())
                        .globalBalance((CurrencyCollection) Tlb.load(CurrencyCollection.class, cs))
                        .build();
            case "McBlockExtra":
                assert skipMagic || (cs.loadUint(16).longValue() == 0xcca5L) : "McBlockExtra: magic not equal to 0xcca5";
                return McBlockExtra.builder()
                        .magic(0xcca5L)
                        .keyBlock(cs.loadBit())
                        .shardHashes(cs.loadDict(32, k -> k.readInt(32), v -> v))
                        .shardFees(cs.loadDict(92, k -> k.readInt(92), v -> v))
                        .build();
            case "CurrencyCollection":
                return CurrencyCollection.builder()
                        .coins(cs.loadCoins())
                        .extraCurrencies(cs.loadDictE(32, k -> k.readInt(32), v -> v))
                        .build();
            case "GlobalVersion":
                assert skipMagic || (cs.loadUint(8).longValue() == 0xc4L) : "GlobalVersion: magic not equal to 0xc4";
                return GlobalVersion.builder()
                        .magic(0xc4L)
                        .version(cs.loadUint(32).longValue())
                        .capabilities(cs.loadUint(64))
                        .build();
            case "ExtBlkRef":
                return ExtBlkRef.builder()
                        .endLt(cs.loadUint(64))
                        .seqno(cs.loadUint(32).intValue())
                        .rootHash(cs.loadBytes(256))
                        .fileHash(cs.loadBytes(256))
                        .build();
            case "BlockInfoPart":
                assert skipMagic || (cs.loadUint(32).longValue() == 0x9bc7a987L) : "BlockInfoPart: magic not equal to 0x9bc7a987";
                return BlockInfoPart.builder()
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
                        .shard((ShardIdent) Tlb.load(ShardIdent.class, cs))
                        .genuTime(cs.loadUint(32).longValue())
                        .startLt(cs.loadUint(64))
                        .endLt(cs.loadUint(64))
                        .genValidatorListHashShort(cs.loadUint(32).longValue())
                        .genCatchainSeqno(cs.loadUint(32).longValue())
                        .minRefMcSeqno(cs.loadUint(32).longValue())
                        .prevKeyBlockSeqno(cs.loadUint(32).longValue())
                        .build();
            case "BlockHandle":
                return BlockHandle.builder()
                        .offset(cs.loadUint(64))
                        .size(cs.loadUint(64))
                        .build();
            case "BlockExtra":
                assert skipMagic || (cs.loadUint(32).longValue() == 0x4a33f6fdL) : "BlockExtra: magic not equal to 0x4a33f6fd";
                return BlockExtra.builder()
                        .magic(0x4a33f6fdL)
                        .inMsgDesc(cs.loadRef())
                        .outMsgDesc(cs.loadRef())
                        .shardAccountBlocks(cs.loadRef())
                        .randSeed(cs.loadBytes(256))
                        .createdBy(cs.loadBytes(256))
                        .custom(isNull(cs.preloadMaybeRefX()) ? null : (McBlockExtra) Tlb.load(McBlockExtra.class, CellSlice.beginParse(cs.loadRef())))
                        .build();
            case "Block":
                assert skipMagic || (cs.loadUint(32).longValue() == 0x11ef55aaL) : "Block: magic not equal to 0x11ef55aa";
                return Block.builder()
                        .magic(0x11ef55aaL)
                        .globalId(cs.loadInt(32).intValue())
                        .blockInfo((BlockHeader) Tlb.load(BlockHeader.class, CellSlice.beginParse(cs.loadRef())))
                        .valueFlow(cs.loadRef())
                        .stateUpdate((StateUpdate) Tlb.load(StateUpdate.class, CellSlice.beginParse(cs.loadRef())))
                        .extra((BlockExtra) Tlb.load(BlockExtra.class, CellSlice.beginParse(cs.loadRef())))
                        .build();
            case "AccountBlock":
                assert skipMagic || (cs.loadUint(32).longValue() == 0x5L) : "AccountBlock: magic not equal to 0x5";
                return AccountBlock.builder()
                        .magic(0x5)
                        .addr(cs.loadBytes(256))
                        .transactions(cs.loadDictE(64, k -> k.readInt(64), v -> v))
                        .stateUpdate(cs.loadRef())
                        .build();
            case "BlockHeader":
                BlockInfoPart infoPart = (BlockInfoPart) Tlb.load(BlockInfoPart.class, cs);
                GlobalVersion globalVersion = ((infoPart.getFlags() & 0x1L) == 1) ? (GlobalVersion) Tlb.load(GlobalVersion.class, cs) : null;
                ExtBlkRef masterRef = infoPart.isNotMaster() ? (ExtBlkRef) Tlb.load(ExtBlkRef.class, CellSlice.beginParse(cs.loadRef())) : null;
                BlkPrevInfo prevRef = loadBlkPrevInfo(CellSlice.beginParse(cs.loadRef()), infoPart.isAfterMerge());
                BlkPrevInfo prevVertRef = infoPart.isVertSeqnoIncr() ? loadBlkPrevInfo(CellSlice.beginParse(cs.loadRef()), false) : null;

                return BlockHeader.builder()
                        .blockInfoPart(infoPart)
                        .genSoftware(globalVersion)
                        .masterRef(masterRef)
                        .prevRef(prevRef)
                        .prevVertRef(prevVertRef)
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

            case "Message": {
                if (isNull(cs)) {
                    return Message.builder().build();
                }
                boolean isExternal = cs.preloadBit();
                if (!isExternal) {
                    InternalMessage internalMessage = (InternalMessage) Tlb.load(InternalMessage.class, cs);

                    return Message.builder()
                            .msgType("INTERNAL")
                            .msg(AnyMessage.builder()
                                    .payload(internalMessage.getBody())
                                    .destAddr(internalMessage.getDstAddr())
                                    .senderAddr(internalMessage.getSrcAddr())
                                    .build())
                            .build();
                } else {
                    boolean isOut = cs.preloadBitAt(2);
                    if (isOut) {
                        ExternalMessageOut externalMessageOut = (ExternalMessageOut) Tlb.load(ExternalMessageOut.class, cs);
                        return Message.builder()
                                .msgType("EXTERNAL_OUT")
                                .msg(AnyMessage.builder()
                                        .payload(externalMessageOut.getBody())
                                        .destAddr(externalMessageOut.getDstAddr())
                                        .senderAddr(externalMessageOut.getSrcAddr())
                                        .build())
                                .build();
                    } else {
                        ExternalMessage externalMessage = (ExternalMessage) Tlb.load(ExternalMessage.class, cs);
                        return Message.builder()
                                .msgType("EXTERNAL_IN")
                                .msg(AnyMessage.builder()
                                        .payload(externalMessage.getBody())
                                        .destAddr(externalMessage.getDstAddr())
                                        .senderAddr(externalMessage.getSrcAddr())
                                        .build())
                                .build();
                    }
                }
                //throw new Error("Unknown msg type ");
            }
            case "MessagesList":
                if (isNull(cs)) {
                    return MessagesList.builder().build();
                }
                return MessagesList.builder()
                        .list(cs.loadDictE(15, k -> k.readInt(15), v -> v))
                        .build();
            case "ComputeSkipReason": {
                int pfx = cs.loadUint(2).intValue();

                switch (pfx) {
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
                throw new Error("unknown compute skip reason");
            }
            case "ComputePhase": {
                boolean isNotSkipped = cs.loadBit();
                if (isNotSkipped) {
                    ComputePhaseVM phase = (ComputePhaseVM) Tlb.load(ComputePhaseVM.class, cs, true);
                    return ComputePhase.builder().phase(phase).build();
                }
                ComputePhaseSkipped phase = (ComputePhaseSkipped) Tlb.load(ComputePhaseSkipped.class, cs, true);
                return ComputePhase.builder().phase(phase).build();
            }
            case "BouncePhaseNegFounds": {
                assert skipMagic || (cs.loadUint(1).intValue() == 0b0) : "BouncePhaseNegFounds: magic not equal to 0b00";
                return BouncePhaseNegFounds.builder().build();
            }
            case "BouncePhaseNoFounds": {
                assert skipMagic || (cs.loadUint(2).intValue() == 0b01) : "BouncePhaseNoFounds: magic not equal to 0b01";
                return BouncePhaseNoFounds.builder()
                        .msgSize((StorageUsedShort) Tlb.load(StorageUsedShort.class, cs))
                        .reqFwdFees(cs.loadCoins())
                        .build();
            }
            case "BouncePhaseok": {
                assert skipMagic || (cs.loadUint(1).intValue() == 0b1) : "BouncePhaseok: magic not equal to 0b1";
                return BouncePhaseok.builder()
                        .magic(0b1)
                        .msgSize((StorageUsedShort) Tlb.load(StorageUsedShort.class, cs))
                        .msgFees(cs.loadCoins())
                        .fwdFees(cs.loadCoins())
                        .build();
            }
            case "BouncePhase": {
                boolean isOk = cs.loadBit();
                if (isOk) {
                    BouncePhaseok phase = (BouncePhaseok) Tlb.load(BouncePhaseok.class, cs, true);
                    return BouncePhase.builder().phase(phase).build();
                }
                boolean isNoFunds = cs.loadBit();
                if (isNoFunds) {
                    BouncePhaseNoFounds phase = (BouncePhaseNoFounds) Tlb.load(BouncePhaseNoFounds.class, cs, true);
                    return BouncePhase.builder().phase(phase).build();
                }
                BouncePhaseNegFounds phase = (BouncePhaseNegFounds) Tlb.load(BouncePhaseNegFounds.class, cs, true);
                return BouncePhase.builder().phase(phase).build();
            }
            case "StoragePhase": {
                return StoragePhase.builder()
                        .storageFeesCollected(cs.loadCoins())
                        .storageFeesDue(cs.loadBit() ? cs.loadCoins() : null)
                        .statusChange((AccStatusChange) Tlb.load(AccStatusChange.class, cs))
                        .build();
            }
            case "ActionPhase": {
                return ActionPhase.builder()
                        .success(cs.loadBit())
                        .valid(cs.loadBit())
                        .noFunds(cs.loadBit())
                        .totalFwdFees(cs.loadBit() ? cs.loadCoins() : null)
                        .totalActionFees(cs.loadBit() ? cs.loadCoins() : null)
                        .resultCode(cs.loadUint(32).longValue())
                        .resultCode(cs.loadBit() ? cs.loadUint(32).longValue() : 0)
                        .totalActions(cs.loadUint(16).longValue())
                        .specActions(cs.loadUint(16).longValue())
                        .skippedActions(cs.loadUint(16).longValue())
                        .messagesCreated(cs.loadUint(16).longValue())
                        .actionListHash(cs.loadBytes(256))
                        .totalMsgSize((StorageUsedShort) Tlb.load(StorageUsedShort.class, cs))
                        .build();
            }

            case "StorageUsedShort": {
                return StorageUsedShort.builder()
                        .cells(cs.loadUint(7))
                        .bits(cs.loadUint(7))
                        .build();
            }
            case "ComputePhaseSkipped": {
                assert skipMagic || (cs.loadUint(1).intValue() == 0b0) : "ComputePhaseSkipped: magic not equal to 0b0";
                return ComputePhaseSkipped.builder()
                        .magic(0)
                        .reason((ComputeSkipReason) Tlb.load(ComputeSkipReason.class, cs))
                        .build();
            }
            case "ComputePhaseVM": {
                assert skipMagic || (cs.loadUint(1).intValue() == 0b1) : "ComputePhaseVM: magic not equal to 0b1";
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
                        .gasUsed(cs.loadUint(7))
                        .gasLimit(cs.loadUint(7))
                        .gasCredit(cs.loadBit() ? cs.loadUint(3) : BigInteger.ZERO)
                        .mode(cs.loadUint(8).intValue())
                        .exitCode(cs.loadUint(32).longValue())
                        .exitArg(cs.loadBit() ? cs.loadUint(32).longValue() : 0L)
                        .vMSteps(cs.loadUint(32).longValue())
                        .vMInitStateHash(cs.loadBytes(256))
                        .vMFinalStateHash(cs.loadBytes(256))
                        .build();
            }
            case "CreditPhase": {
                return CreditPhase.builder()
                        .dueFeesCollected(cs.loadBit() ? cs.loadCoins() : BigInteger.ZERO)
                        .credit((CurrencyCollection) Tlb.load(CurrencyCollection.class, cs))
                        .build();
            }
            case "SplitMergeInfo": {
                return SplitMergeInfo.builder()
                        .curShardPfxLen(cs.loadUint(6).intValue())
                        .accSplitDepth(cs.loadUint(6).intValue())
                        .thisAddr(cs.loadBytes(256))
                        .siblingAddr(cs.loadBytes(256))
                        .build();
            }
            case "Transaction": {
                assert skipMagic || (cs.loadUint(4).intValue() == 0b0111) : "Transaction: magic not equal to 0b0111";

                Transaction tx = Transaction.builder()
                        .magic(0b0111)
                        .accountAddr(cs.loadBytes(256))
                        .lt(cs.loadUint(64))
                        .prevTxHash(cs.loadBytes(256))
                        .prevTxLT(cs.loadUint(64))
                        .now(cs.loadUint(32).longValue())
                        .outMsgCount(cs.loadUint(15).intValue())
                        .origStatus(cs.loadString(2))
                        .endStatus(cs.loadString(2))
                        .build();

                System.out.println("tx " + tx);

                CellSlice io = CellSlice.beginParse(cs.loadRef());
                tx.setInOut(TransactionIO.builder()
                        .in((Message) Tlb.load(Message.class, io.loadMaybeRefX()))
                        .out((MessagesList) Tlb.load(MessagesList.class, io.loadRef()))
                        .build());


                for (Map.Entry<Object, Object> entry : tx.getInOut().getOut().getList().elements.entrySet()) {
                    System.out.println("key " + entry.getKey() + ", value " + ((Cell) entry.getValue()).print());
                    Message i = (Message) Tlb.load(Message.class, (Cell) entry.getValue());
                    System.out.println("i = " + i.toString());
                }


                tx.setTotalFees((CurrencyCollection) Tlb.load(CurrencyCollection.class, cs));
                tx.setStateUpdate((HashUpdate) Tlb.load(HashUpdate.class, CellSlice.beginParse(cs.loadRef())));
                tx.setDescription((TransactionDescription) Tlb.load(TransactionDescription.class, CellSlice.beginParse(cs.loadRef())));

                return tx;
            }
            case "HashUpdate": {
                assert cs.loadUint(8).intValue() == 0x72 : "HashUpdate: magic not equal to 0x72";
                return HashUpdate.builder()
                        .magic(0x72)
                        .oldHash(cs.loadBytes(256))
                        .newHash(cs.loadBytes(256))
                        .build();
            }
            case "TransactionDescription": {
                int pfx = cs.loadUint(3).intValue();
                switch (pfx) {
                    case 0b000 -> {
                        boolean isStorage = cs.loadBit();
                        if (isStorage) {
                            TransactionDescriptionStorage desc = (TransactionDescriptionStorage) Tlb.load(TransactionDescriptionStorage.class, cs, true);
                            return TransactionDescription.builder().description(desc).build();
                        }
                        TransactionDescriptionOrdinary descOrdinary = (TransactionDescriptionOrdinary) Tlb.load(TransactionDescriptionOrdinary.class, cs, true);
                        return TransactionDescription.builder().description(descOrdinary).build();
                    }
                    case 0b001 -> {
                        TransactionDescriptionTickTock descTickTock = (TransactionDescriptionTickTock) Tlb.load(TransactionDescriptionTickTock.class, cs, true);
                        return TransactionDescription.builder().description(descTickTock).build();
                    }
                    case 0b010 -> {
                        boolean isInstall = cs.loadBit();
                        if (isInstall) {
                            TransactionDescriptionSplitInstall descSplit = (TransactionDescriptionSplitInstall) Tlb.load(TransactionDescriptionSplitInstall.class, cs, true);
                            return TransactionDescription.builder().description(descSplit).build();
                        }
                        TransactionDescriptionSplitPrepare descSplitPrepare = (TransactionDescriptionSplitPrepare) Tlb.load(TransactionDescriptionSplitPrepare.class, cs, true);
                        return TransactionDescription.builder().description(descSplitPrepare).build();
                    }
                    case 0b011 -> {
                        boolean isInstall = cs.loadBit();
                        if (isInstall) {
                            TransactionDescriptionMergeInstall descMerge = (TransactionDescriptionMergeInstall) Tlb.load(TransactionDescriptionSplitInstall.class, cs, true);
                            return TransactionDescription.builder().description(descMerge).build();
                        }
                        TransactionDescriptionMergePrepare descMergePrepare = (TransactionDescriptionMergePrepare) Tlb.load(TransactionDescriptionMergePrepare.class, cs, true);
                        return TransactionDescription.builder().description(descMergePrepare).build();
                    }
                }
                throw new Error("unknown transaction description type (must be in range [0..3], current " + pfx);
            }
            case "TransactionDescriptionStorage": {
                assert cs.loadUint(4).intValue() == 0b0001 : "TransactionDescriptionStorage: magic not equal to 0b0001";
                return TransactionDescriptionStorage.builder()
                        .magic(0b0001)
                        .storagePhase((StoragePhase) Tlb.load(StoragePhase.class, cs))
                        .build();
            }
            case "TransactionDescriptionOrdinary": {
                assert skipMagic || (cs.loadUint(4).intValue() == 0b0000) : "TransactionDescriptionOrdinary: magic not equal to 0b0000";
                return TransactionDescriptionOrdinary.builder()
                        .magic(0b0000)
                        .creditFirst(cs.loadBit())
                        .storagePhase(cs.loadBit() ? (StoragePhase) Tlb.load(StoragePhase.class, cs) : null)
                        .creditPhase(cs.loadBit() ? (CreditPhase) Tlb.load(CreditPhase.class, cs) : null)
                        .computePhase((ComputePhase) Tlb.load(ComputePhase.class, cs))
                        .actionPhase((ActionPhase) Tlb.load(ActionPhase.class, cs.loadMaybeRefX()))
                        .aborted(cs.loadBit())
                        .bouncePhase(cs.loadBit() ? (BouncePhase) Tlb.load(BouncePhase.class, cs) : null)
                        .destroyed(cs.loadBit())
                        .build();


            }
            case "TransactionDescriptionTickTock": {
                assert skipMagic || (cs.loadUint(3).intValue() == 0b001) : "TransactionDescriptionTickTock: magic not equal to 0b001";
                return TransactionDescriptionTickTock.builder()
                        .magic(0b001)
                        .isTock(cs.loadBit())
                        .storagePhase((StoragePhase) Tlb.load(StoragePhase.class, cs))
                        .computePhase((ComputePhase) Tlb.load(ComputePhase.class, cs))
                        .actionPhase((ActionPhase) Tlb.load(ActionPhase.class, cs.loadMaybeRefX()))
                        .aborted(cs.loadBit())
                        .destroyed(cs.loadBit())
                        .build();
            }
            case "TransactionDescriptionSplitInstall": {
                assert skipMagic || (cs.loadUint(4).intValue() == 0b0101) : "TransactionDescriptionSplitInstall: magic not equal to 0b0101";
                return TransactionDescriptionSplitInstall.builder()
                        .magic(0b0101)
                        .splitInfo((SplitMergeInfo) Tlb.load(SplitMergeInfo.class, cs))
                        .prepareTransaction((Transaction) Tlb.load(Transaction.class, cs.loadRef()))
                        .installed(cs.loadBit())
                        .build();
            }
            case "TransactionDescriptionSplitPrepare": {
                assert skipMagic || (cs.loadUint(4).intValue() == 0b0100) : "TransactionDescriptionSplitPrepare: magic not equal to 0b0100";
                return TransactionDescriptionMergeInstall.builder()
                        .magic(0b0100)
                        .splitInfo((SplitMergeInfo) Tlb.load(SplitMergeInfo.class, cs))
                        .storagePhase(cs.loadBit() ? (StoragePhase) Tlb.load(StoragePhase.class, cs) : null)
                        .computePhase((ComputePhase) Tlb.load(ComputePhase.class, cs))
                        .actionPhase((ActionPhase) Tlb.load(ActionPhase.class, cs.loadMaybeRefX()))
                        .aborted(cs.loadBit())
                        .destroyed(cs.loadBit())
                        .build();
            }
            case "TransactionDescriptionMergeInstall": {
                assert skipMagic || (cs.loadUint(4).intValue() == 0b0111) : "TransactionDescriptionMergeInstall: magic not equal to 0b0111";
                return TransactionDescriptionMergeInstall.builder()
                        .magic(0b0111)
                        .splitInfo((SplitMergeInfo) Tlb.load(SplitMergeInfo.class, cs))
                        .prepareTransaction((Transaction) Tlb.load(Transaction.class, cs.loadRef()))
                        .storagePhase(cs.loadBit() ? (StoragePhase) Tlb.load(StoragePhase.class, cs) : null)
                        .creditPhase(cs.loadBit() ? (CreditPhase) Tlb.load(CreditPhase.class, cs) : null)
                        .computePhase((ComputePhase) Tlb.load(ComputePhase.class, cs))
                        .actionPhase((ActionPhase) Tlb.load(ActionPhase.class, cs.loadMaybeRefX()))
                        .aborted(cs.loadBit())
                        .destroyed(cs.loadBit())
                        .build();
            }
            case "TransactionDescriptionMergePrepare": {
                assert skipMagic || (cs.loadUint(4).intValue() == 0b0110) : "TransactionDescriptionMergePrepare: magic not equal to 0b0110";
                return TransactionDescriptionMergePrepare.builder()
                        .magic(0b0110)
                        .splitInfo((SplitMergeInfo) Tlb.load(SplitMergeInfo.class, cs)) // todo
                        .storagePhase((StoragePhase) Tlb.load(StoragePhase.class, cs)) // todo
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
                assert skipMagic || (!cs.loadBit()) : "InternalMessage: magic not equal to 0";
                return InternalMessage.builder()
                        .magic(0L)
                        .iHRDisabled(cs.loadBit())
                        .bounce(cs.loadBit())
                        .bounced(cs.loadBit())
                        .srcAddr(cs.loadAddress())
                        .dstAddr(cs.loadAddress())
                        .value((CurrencyCollection) Tlb.load(CurrencyCollection.class, cs))
                        .iHRFee(cs.loadCoins())
                        .fwdFee(cs.loadCoins())
                        .createdLt(cs.loadUint(64))
                        .createdAt(cs.loadUint(32).longValue())
                        .stateInit(cs.loadBit() ? (cs.loadBit() ? (StateInit) Tlb.load(StateInit.class, CellSlice.beginParse(cs.loadRef())) : (StateInit) Tlb.load(StateInit.class, cs)) : null) //review
                        .body(cs.loadBit() ? cs.loadRef() : CellBuilder.beginCell().storeBitString(cs.loadBits(cs.getRestBits()))) // todo
                        .build();
            case "ExternalMessage":
                assert skipMagic || (cs.loadUint(2).longValue() == 0x2L) : "ExternalMessage: magic not equal to 0x2";
                return ExternalMessage.builder()
                        .magic(2L)
                        .srcAddr(cs.loadAddress())
                        .dstAddr(cs.loadAddress())
                        .importFee(cs.loadCoins())
                        .stateInit(cs.loadBit() ? (cs.loadBit() ? (StateInit) Tlb.load(StateInit.class, CellSlice.beginParse(cs.loadRef())) : (StateInit) Tlb.load(StateInit.class, cs)) : null) //review
                        .body(cs.loadBit() ? cs.loadRef() : CellBuilder.beginCell().storeBitString(cs.loadBits(cs.getRestBits())))
                        .build();
            case "ExternalMessageOut":
                assert skipMagic || (cs.loadUint(2).longValue() == 0x3L) : "ExternalMessageOut: magic not equal to 0x3";
                return ExternalMessageOut.builder()
                        .magic(3L)
                        .srcAddr(cs.loadAddress())
                        .dstAddr(cs.loadAddress())
                        .createdLt(cs.loadUint(64))
                        .createdAt(cs.loadUint(32).longValue())
                        .stateInit((StateInit) Tlb.load(StateInit.class, cs))
                        .body(cs.loadMaybeRefX())
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
            ExtBlkRef blkRef = (ExtBlkRef) Tlb.load(ExtBlkRef.class, cs);
            blkPrevInfo.setPrev1(blkRef);
            return blkPrevInfo;
        }

        ExtBlkRef blkRef1 = (ExtBlkRef) Tlb.load(ExtBlkRef.class, CellSlice.beginParse(cs.loadRef()));
        ExtBlkRef blkRef2 = (ExtBlkRef) Tlb.load(ExtBlkRef.class, CellSlice.beginParse(cs.loadRef()));
        blkPrevInfo.setPrev1(blkRef1);
        blkPrevInfo.setPrev2(blkRef2);
        return blkPrevInfo;
    }
}
