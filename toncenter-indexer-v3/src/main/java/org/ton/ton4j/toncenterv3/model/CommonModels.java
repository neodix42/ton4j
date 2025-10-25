package org.ton.ton4j.toncenterv3.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Common model classes used across multiple endpoints
 */
public class CommonModels {
    
    @Data
    public static class BlockId {
        @SerializedName("workchain")
        private Integer workchain;
        
        @SerializedName("shard")
        private String shard;
        
        @SerializedName("seqno")
        private Integer seqno;
    }
    
    @Data
    public static class AccountState {
        @SerializedName("hash")
        private String hash;
        
        @SerializedName("balance")
        private String balance;
        
        @SerializedName("account_status")
        private String accountStatus;
        
        @SerializedName("frozen_hash")
        private String frozenHash;
        
        @SerializedName("code_hash")
        private String codeHash;
        
        @SerializedName("data_hash")
        private String dataHash;
        
        @SerializedName("code_boc")
        private String codeBoc;
        
        @SerializedName("data_boc")
        private String dataBoc;
        
        @SerializedName("extra_currencies")
        private Map<String, String> extraCurrencies;
    }
    
    @Data
    public static class Message {
        @SerializedName("hash")
        private String hash;
        
        @SerializedName("hash_norm")
        private String hashNorm;
        
        @SerializedName("source")
        private String source;
        
        @SerializedName("destination")
        private String destination;
        
        @SerializedName("value")
        private String value;
        
        @SerializedName("fwd_fee")
        private String fwdFee;
        
        @SerializedName("ihr_fee")
        private String ihrFee;
        
        @SerializedName("created_lt")
        private String createdLt;
        
        @SerializedName("created_at")
        private String createdAt;
        
        @SerializedName("opcode")
        private Integer opcode;
        
        @SerializedName("decoded_opcode")
        private String decodedOpcode;
        
        @SerializedName("ihr_disabled")
        private Boolean ihrDisabled;
        
        @SerializedName("bounce")
        private Boolean bounce;
        
        @SerializedName("bounced")
        private Boolean bounced;
        
        @SerializedName("import_fee")
        private String importFee;
        
        @SerializedName("message_content")
        private MessageContent messageContent;
        
        @SerializedName("init_state")
        private MessageContent initState;
        
        @SerializedName("body_hash")
        private String bodyHash;
        
        @SerializedName("value_extra_currencies")
        private Map<String, String> valueExtraCurrencies;
        
        @SerializedName("in_msg_tx_hash")
        private String inMsgTxHash;
        
        @SerializedName("out_msg_tx_hash")
        private String outMsgTxHash;
        
        @SerializedName("extra_flags")
        private String extraFlags;
    }
    
    @Data
    public static class MessageContent {
        @SerializedName("hash")
        private String hash;
        
        @SerializedName("body")
        private String body;
        
        @SerializedName("decoded")
        private Object decoded;
    }
    
    @Data
    public static class Transaction {
        @SerializedName("account")
        private String account;
        
        @SerializedName("hash")
        private String hash;
        
        @SerializedName("lt")
        private String lt;
        
        @SerializedName("now")
        private Integer now;
        
        @SerializedName("orig_status")
        private String origStatus;
        
        @SerializedName("end_status")
        private String endStatus;
        
        @SerializedName("total_fees")
        private String totalFees;
        
        @SerializedName("total_fees_extra_currencies")
        private Map<String, String> totalFeesExtraCurrencies;
        
        @SerializedName("prev_trans_hash")
        private String prevTransHash;
        
        @SerializedName("prev_trans_lt")
        private String prevTransLt;
        
        @SerializedName("description")
        private TransactionDescr description;
        
        @SerializedName("block_ref")
        private BlockId blockRef;
        
        @SerializedName("in_msg")
        private Message inMsg;
        
        @SerializedName("out_msgs")
        private List<Message> outMsgs;
        
        @SerializedName("account_state_before")
        private AccountState accountStateBefore;
        
        @SerializedName("account_state_after")
        private AccountState accountStateAfter;
        
        @SerializedName("mc_block_seqno")
        private Integer mcBlockSeqno;
        
        @SerializedName("trace_id")
        private String traceId;
        
        @SerializedName("trace_external_hash")
        private String traceExternalHash;
        
        @SerializedName("emulated")
        private Boolean emulated;
    }
    
    @Data
    public static class TransactionDescr {
        @SerializedName("type")
        private String type;
        
        @SerializedName("credit_first")
        private Boolean creditFirst;
        
        @SerializedName("storage_ph")
        private StoragePhase storagePh;
        
        @SerializedName("credit_ph")
        private CreditPhase creditPh;
        
        @SerializedName("compute_ph")
        private ComputePhase computePh;
        
        @SerializedName("action")
        private ActionPhase action;
        
        @SerializedName("aborted")
        private Boolean aborted;
        
        @SerializedName("bounce")
        private BouncePhase bounce;
        
        @SerializedName("destroyed")
        private Boolean destroyed;
        
        @SerializedName("split_info")
        private SplitInfo splitInfo;
        
        @SerializedName("installed")
        private Boolean installed;
        
        @SerializedName("is_tock")
        private Boolean isTock;
    }
    
    @Data
    public static class StoragePhase {
        @SerializedName("storage_fees_collected")
        private String storageFeesCollected;
        
        @SerializedName("storage_fees_due")
        private String storageFeesDue;
        
        @SerializedName("status_change")
        private String statusChange;
    }
    
    @Data
    public static class CreditPhase {
        @SerializedName("due_fees_collected")
        private String dueFeesCollected;
        
        @SerializedName("credit")
        private String credit;
        
        @SerializedName("credit_extra_currencies")
        private Map<String, String> creditExtraCurrencies;
    }
    
    @Data
    public static class ComputePhase {
        @SerializedName("skipped")
        private Boolean skipped;
        
        @SerializedName("success")
        private Boolean success;
        
        @SerializedName("msg_state_used")
        private Boolean msgStateUsed;
        
        @SerializedName("account_activated")
        private Boolean accountActivated;
        
        @SerializedName("gas_fees")
        private String gasFees;
        
        @SerializedName("gas_used")
        private String gasUsed;
        
        @SerializedName("gas_limit")
        private String gasLimit;
        
        @SerializedName("gas_credit")
        private String gasCredit;
        
        @SerializedName("mode")
        private Integer mode;
        
        @SerializedName("exit_code")
        private Integer exitCode;
        
        @SerializedName("exit_arg")
        private Integer exitArg;
        
        @SerializedName("vm_steps")
        private Integer vmSteps;
        
        @SerializedName("vm_init_state_hash")
        private String vmInitStateHash;
        
        @SerializedName("vm_final_state_hash")
        private String vmFinalStateHash;
        
        @SerializedName("reason")
        private String reason;
    }
    
    @Data
    public static class ActionPhase {
        @SerializedName("success")
        private Boolean success;
        
        @SerializedName("valid")
        private Boolean valid;
        
        @SerializedName("no_funds")
        private Boolean noFunds;
        
        @SerializedName("status_change")
        private String statusChange;
        
        @SerializedName("total_fwd_fees")
        private String totalFwdFees;
        
        @SerializedName("total_action_fees")
        private String totalActionFees;
        
        @SerializedName("result_code")
        private Integer resultCode;
        
        @SerializedName("result_arg")
        private Integer resultArg;
        
        @SerializedName("tot_actions")
        private Integer totActions;
        
        @SerializedName("spec_actions")
        private Integer specActions;
        
        @SerializedName("skipped_actions")
        private Integer skippedActions;
        
        @SerializedName("msgs_created")
        private Integer msgsCreated;
        
        @SerializedName("action_list_hash")
        private String actionListHash;
        
        @SerializedName("tot_msg_size")
        private MsgSize totMsgSize;
    }
    
    @Data
    public static class BouncePhase {
        @SerializedName("type")
        private String type;
        
        @SerializedName("msg_size")
        private MsgSize msgSize;
        
        @SerializedName("req_fwd_fees")
        private String reqFwdFees;
        
        @SerializedName("msg_fees")
        private String msgFees;
        
        @SerializedName("fwd_fees")
        private String fwdFees;
    }
    
    @Data
    public static class SplitInfo {
        @SerializedName("cur_shard_pfx_len")
        private Integer curShardPfxLen;
        
        @SerializedName("acc_split_depth")
        private Integer accSplitDepth;
        
        @SerializedName("this_addr")
        private String thisAddr;
        
        @SerializedName("sibling_addr")
        private String siblingAddr;
    }
    
    @Data
    public static class MsgSize {
        @SerializedName("cells")
        private String cells;
        
        @SerializedName("bits")
        private String bits;
    }
    
    @Data
    public static class Block {
        @SerializedName("workchain")
        private Integer workchain;
        
        @SerializedName("shard")
        private String shard;
        
        @SerializedName("seqno")
        private Integer seqno;
        
        @SerializedName("root_hash")
        private String rootHash;
        
        @SerializedName("file_hash")
        private String fileHash;
        
        @SerializedName("global_id")
        private Integer globalId;
        
        @SerializedName("version")
        private Integer version;
        
        @SerializedName("after_merge")
        private Boolean afterMerge;
        
        @SerializedName("before_split")
        private Boolean beforeSplit;
        
        @SerializedName("after_split")
        private Boolean afterSplit;
        
        @SerializedName("want_split")
        private Boolean wantSplit;
        
        @SerializedName("want_merge")
        private Boolean wantMerge;
        
        @SerializedName("key_block")
        private Boolean keyBlock;
        
        @SerializedName("vert_seqno_incr")
        private Boolean vertSeqnoIncr;
        
        @SerializedName("flags")
        private Integer flags;
        
        @SerializedName("gen_utime")
        private String genUtime;
        
        @SerializedName("start_lt")
        private String startLt;
        
        @SerializedName("end_lt")
        private String endLt;
        
        @SerializedName("validator_list_hash_short")
        private Integer validatorListHashShort;
        
        @SerializedName("gen_catchain_seqno")
        private Integer genCatchainSeqno;
        
        @SerializedName("min_ref_mc_seqno")
        private Integer minRefMcSeqno;
        
        @SerializedName("prev_key_block_seqno")
        private Integer prevKeyBlockSeqno;
        
        @SerializedName("vert_seqno")
        private Integer vertSeqno;
        
        @SerializedName("master_ref_seqno")
        private Integer masterRefSeqno;
        
        @SerializedName("rand_seed")
        private String randSeed;
        
        @SerializedName("created_by")
        private String createdBy;
        
        @SerializedName("tx_count")
        private Integer txCount;
        
        @SerializedName("masterchain_block_ref")
        private BlockId masterchainBlockRef;
        
        @SerializedName("prev_blocks")
        private List<BlockId> prevBlocks;
    }
}
