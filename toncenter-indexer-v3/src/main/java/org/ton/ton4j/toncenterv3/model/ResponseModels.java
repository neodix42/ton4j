package org.ton.ton4j.toncenterv3.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;
import java.util.Map;
import org.ton.ton4j.toncenterv3.model.CommonModels.*;

/**
 * Response model classes for all TonCenter V3 API endpoints
 */
public class ResponseModels {
    
    // ========== ACCOUNT RESPONSES ==========
    
    @Data
    public static class AccountStatesResponse {
        @SerializedName("accounts")
        private List<AccountStateFull> accounts;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
        
        @SerializedName("metadata")
        private Map<String, Metadata.AddressMetadata> metadata;
    }
    
    @Data
    public static class AccountStateFull {
        @SerializedName("address")
        private String address;
        
        @SerializedName("balance")
        private String balance;
        
        @SerializedName("extra_currencies")
        private Map<String, String> extraCurrencies;
        
        @SerializedName("last_transaction_lt")
        private String lastTransactionLt;
        
        @SerializedName("last_transaction_hash")
        private String lastTransactionHash;
        
        @SerializedName("status")
        private String status;
        
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
        
        @SerializedName("account_state_hash")
        private String accountStateHash;
        
        @SerializedName("interfaces")
        private List<String> interfaces;
        
        @SerializedName("contract_methods")
        private List<Integer> contractMethods;
    }
    
    @Data
    public static class WalletStatesResponse {
        @SerializedName("wallets")
        private List<WalletState> wallets;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
        
        @SerializedName("metadata")
        private Map<String, Metadata.AddressMetadata> metadata;
    }
    
    @Data
    public static class WalletState {
        @SerializedName("address")
        private String address;
        
        @SerializedName("balance")
        private String balance;
        
        @SerializedName("extra_currencies")
        private Map<String, String> extraCurrencies;
        
        @SerializedName("last_transaction_lt")
        private String lastTransactionLt;
        
        @SerializedName("last_transaction_hash")
        private String lastTransactionHash;
        
        @SerializedName("status")
        private String status;
        
        @SerializedName("wallet_type")
        private String walletType;
        
        @SerializedName("wallet_id")
        private Integer walletId;
        
        @SerializedName("seqno")
        private Integer seqno;
        
        @SerializedName("is_wallet")
        private Boolean isWallet;
        
        @SerializedName("code_hash")
        private String codeHash;
        
        @SerializedName("is_signature_allowed")
        private Boolean isSignatureAllowed;
    }
    
    // ========== BLOCKCHAIN RESPONSES ==========
    
    @Data
    public static class BlocksResponse {
        @SerializedName("blocks")
        private List<Block> blocks;
    }
    
    @Data
    public static class TransactionsResponse {
        @SerializedName("transactions")
        private List<Transaction> transactions;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
    }
    
    @Data
    public static class MessagesResponse {
        @SerializedName("messages")
        private List<Message> messages;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
        
        @SerializedName("metadata")
        private Map<String, Metadata.AddressMetadata> metadata;
    }
    
    @Data
    public static class MasterchainInfo {
        @SerializedName("first")
        private Block first;
        
        @SerializedName("last")
        private Block last;
    }
    
    // ========== ACTION RESPONSES ==========
    
    @Data
    public static class ActionsResponse {
        @SerializedName("actions")
        private List<Action> actions;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
        
        @SerializedName("metadata")
        private Map<String, Metadata.AddressMetadata> metadata;
    }
    
    @Data
    public static class Action {
        @SerializedName("action_id")
        private String actionId;
        
        @SerializedName("type")
        private String type;
        
        @SerializedName("success")
        private Boolean success;
        
        @SerializedName("start_utime")
        private Integer startUtime;
        
        @SerializedName("end_utime")
        private Integer endUtime;
        
        @SerializedName("start_lt")
        private String startLt;
        
        @SerializedName("end_lt")
        private String endLt;
        
        @SerializedName("trace_id")
        private String traceId;
        
        @SerializedName("trace_external_hash")
        private String traceExternalHash;
        
        @SerializedName("trace_external_hash_norm")
        private String traceExternalHashNorm;
        
        @SerializedName("trace_end_utime")
        private Integer traceEndUtime;
        
        @SerializedName("trace_end_lt")
        private String traceEndLt;
        
        @SerializedName("trace_mc_seqno_end")
        private Integer traceMcSeqnoEnd;
        
        @SerializedName("transactions")
        private List<String> transactions;
        
        @SerializedName("transactions_full")
        private List<Transaction> transactionsFull;
        
        @SerializedName("accounts")
        private List<String> accounts;
        
        @SerializedName("details")
        private Object details;
    }
    
    @Data
    public static class TracesResponse {
        @SerializedName("traces")
        private List<Trace> traces;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
        
        @SerializedName("metadata")
        private Map<String, Metadata.AddressMetadata> metadata;
    }
    
    @Data
    public static class Trace {
        @SerializedName("trace_id")
        private String traceId;
        
        @SerializedName("external_hash")
        private String externalHash;
        
        @SerializedName("mc_seqno_start")
        private String mcSeqnoStart;
        
        @SerializedName("mc_seqno_end")
        private String mcSeqnoEnd;
        
        @SerializedName("start_lt")
        private String startLt;
        
        @SerializedName("start_utime")
        private Integer startUtime;
        
        @SerializedName("end_lt")
        private String endLt;
        
        @SerializedName("end_utime")
        private Integer endUtime;
        
        @SerializedName("is_incomplete")
        private Boolean isIncomplete;
        
        @SerializedName("transactions_order")
        private List<String> transactionsOrder;
        
        @SerializedName("transactions")
        private Map<String, Transaction> transactions;
        
        @SerializedName("trace")
        private TraceNode trace;
        
        @SerializedName("trace_info")
        private TraceMeta traceInfo;
        
        @SerializedName("actions")
        private List<Action> actions;
        
        @SerializedName("warning")
        private String warning;
    }
    
    @Data
    public static class TraceNode {
        @SerializedName("tx_hash")
        private String txHash;
        
        @SerializedName("transaction")
        private Transaction transaction;
        
        @SerializedName("in_msg_hash")
        private String inMsgHash;
        
        @SerializedName("in_msg")
        private Message inMsg;
        
        @SerializedName("children")
        private List<TraceNode> children;
    }
    
    @Data
    public static class TraceMeta {
        @SerializedName("trace_state")
        private String traceState;
        
        @SerializedName("classification_state")
        private String classificationState;
        
        @SerializedName("transactions")
        private Integer transactions;
        
        @SerializedName("messages")
        private Integer messages;
        
        @SerializedName("pending_messages")
        private Integer pendingMessages;
    }
    
    // ========== JETTON RESPONSES ==========
    
    @Data
    public static class JettonMastersResponse {
        @SerializedName("jetton_masters")
        private List<JettonMaster> jettonMasters;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
        
        @SerializedName("metadata")
        private Map<String, Metadata.AddressMetadata> metadata;
    }
    
    @Data
    public static class JettonMaster {
        @SerializedName("address")
        private String address;
        
        @SerializedName("total_supply")
        private String totalSupply;
        
        @SerializedName("mintable")
        private Boolean mintable;
        
        @SerializedName("admin_address")
        private String adminAddress;
        
        @SerializedName("jetton_content")
        private Map<String, Object> jettonContent;
        
        @SerializedName("jetton_wallet_code_hash")
        private String jettonWalletCodeHash;
        
        @SerializedName("code_hash")
        private String codeHash;
        
        @SerializedName("data_hash")
        private String dataHash;
        
        @SerializedName("last_transaction_lt")
        private String lastTransactionLt;
    }
    
    @Data
    public static class JettonWalletsResponse {
        @SerializedName("jetton_wallets")
        private List<JettonWallet> jettonWallets;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
        
        @SerializedName("metadata")
        private Map<String, Metadata.AddressMetadata> metadata;
    }
    
    @Data
    public static class JettonWallet {
        @SerializedName("address")
        private String address;
        
        @SerializedName("balance")
        private String balance;
        
        @SerializedName("owner")
        private String owner;
        
        @SerializedName("jetton")
        private String jetton;
        
        @SerializedName("last_transaction_lt")
        private String lastTransactionLt;
        
        @SerializedName("code_hash")
        private String codeHash;
        
        @SerializedName("data_hash")
        private String dataHash;
        
        @SerializedName("mintless_info")
        private JettonWalletMintlessInfo mintlessInfo;
    }
    
    @Data
    public static class JettonWalletMintlessInfo {
        @SerializedName("custom_payload_api_uri")
        private List<String> customPayloadApiUri;
        
        @SerializedName("amount")
        private String amount;
        
        @SerializedName("start_from")
        private Integer startFrom;
        
        @SerializedName("expire_at")
        private Integer expireAt;
    }
    
    @Data
    public static class JettonTransfersResponse {
        @SerializedName("jetton_transfers")
        private List<JettonTransfer> jettonTransfers;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
        
        @SerializedName("metadata")
        private Map<String, Metadata.AddressMetadata> metadata;
    }
    
    @Data
    public static class JettonTransfer {
        @SerializedName("query_id")
        private String queryId;
        
        @SerializedName("source")
        private String source;
        
        @SerializedName("destination")
        private String destination;
        
        @SerializedName("amount")
        private String amount;
        
        @SerializedName("source_wallet")
        private String sourceWallet;
        
        @SerializedName("jetton_master")
        private String jettonMaster;
        
        @SerializedName("transaction_hash")
        private String transactionHash;
        
        @SerializedName("transaction_lt")
        private String transactionLt;
        
        @SerializedName("transaction_now")
        private Integer transactionNow;
        
        @SerializedName("transaction_aborted")
        private Boolean transactionAborted;
        
        @SerializedName("response_destination")
        private String responseDestination;
        
        @SerializedName("custom_payload")
        private String customPayload;
        
        @SerializedName("forward_ton_amount")
        private String forwardTonAmount;
        
        @SerializedName("forward_payload")
        private String forwardPayload;
        
        @SerializedName("decoded_custom_payload")
        private List<Integer> decodedCustomPayload;
        
        @SerializedName("decoded_forward_payload")
        private Object decodedForwardPayload;
        
        @SerializedName("trace_id")
        private String traceId;
    }
    
    @Data
    public static class JettonBurnsResponse {
        @SerializedName("jetton_burns")
        private List<JettonBurn> jettonBurns;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
        
        @SerializedName("metadata")
        private Map<String, Metadata.AddressMetadata> metadata;
    }
    
    @Data
    public static class JettonBurn {
        @SerializedName("query_id")
        private String queryId;
        
        @SerializedName("owner")
        private String owner;
        
        @SerializedName("jetton_wallet")
        private String jettonWallet;
        
        @SerializedName("jetton_master")
        private String jettonMaster;
        
        @SerializedName("amount")
        private String amount;
        
        @SerializedName("response_destination")
        private String responseDestination;
        
        @SerializedName("custom_payload")
        private String customPayload;
        
        @SerializedName("transaction_hash")
        private String transactionHash;
        
        @SerializedName("transaction_lt")
        private String transactionLt;
        
        @SerializedName("transaction_now")
        private Integer transactionNow;
        
        @SerializedName("transaction_aborted")
        private Boolean transactionAborted;
        
        @SerializedName("decoded_custom_payload")
        private List<Integer> decodedCustomPayload;
        
        @SerializedName("trace_id")
        private String traceId;
    }
    
    // ========== NFT RESPONSES ==========
    
    @Data
    public static class NFTCollectionsResponse {
        @SerializedName("nft_collections")
        private List<NFTCollection> nftCollections;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
        
        @SerializedName("metadata")
        private Map<String, Metadata.AddressMetadata> metadata;
    }
    
    @Data
    public static class NFTCollection {
        @SerializedName("address")
        private String address;
        
        @SerializedName("next_item_index")
        private String nextItemIndex;
        
        @SerializedName("owner_address")
        private String ownerAddress;
        
        @SerializedName("collection_content")
        private Map<String, Object> collectionContent;
        
        @SerializedName("code_hash")
        private String codeHash;
        
        @SerializedName("data_hash")
        private String dataHash;
        
        @SerializedName("last_transaction_lt")
        private String lastTransactionLt;
    }
    
    @Data
    public static class NFTItemsResponse {
        @SerializedName("nft_items")
        private List<NFTItem> nftItems;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
        
        @SerializedName("metadata")
        private Map<String, Metadata.AddressMetadata> metadata;
    }
    
    @Data
    public static class NFTItem {
        @SerializedName("address")
        private String address;
        
        @SerializedName("init")
        private Boolean init;
        
        @SerializedName("index")
        private String index;
        
        @SerializedName("collection_address")
        private String collectionAddress;
        
        @SerializedName("owner_address")
        private String ownerAddress;
        
        @SerializedName("content")
        private Map<String, Object> content;
        
        @SerializedName("last_transaction_lt")
        private String lastTransactionLt;
        
        @SerializedName("code_hash")
        private String codeHash;
        
        @SerializedName("data_hash")
        private String dataHash;
        
        @SerializedName("collection")
        private NFTCollection collection;
        
        @SerializedName("on_sale")
        private Boolean onSale;
        
        @SerializedName("sale_contract_address")
        private String saleContractAddress;
        
        @SerializedName("auction_contract_address")
        private String auctionContractAddress;
        
        @SerializedName("real_owner")
        private String realOwner;
    }
    
    @Data
    public static class NFTTransfersResponse {
        @SerializedName("nft_transfers")
        private List<NFTTransfer> nftTransfers;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
        
        @SerializedName("metadata")
        private Map<String, Metadata.AddressMetadata> metadata;
    }
    
    @Data
    public static class NFTTransfer {
        @SerializedName("query_id")
        private String queryId;
        
        @SerializedName("nft_address")
        private String nftAddress;
        
        @SerializedName("nft_collection")
        private String nftCollection;
        
        @SerializedName("old_owner")
        private String oldOwner;
        
        @SerializedName("new_owner")
        private String newOwner;
        
        @SerializedName("response_destination")
        private String responseDestination;
        
        @SerializedName("custom_payload")
        private String customPayload;
        
        @SerializedName("forward_amount")
        private String forwardAmount;
        
        @SerializedName("forward_payload")
        private String forwardPayload;
        
        @SerializedName("transaction_hash")
        private String transactionHash;
        
        @SerializedName("transaction_lt")
        private String transactionLt;
        
        @SerializedName("transaction_now")
        private Integer transactionNow;
        
        @SerializedName("transaction_aborted")
        private Boolean transactionAborted;
        
        @SerializedName("decoded_custom_payload")
        private List<Integer> decodedCustomPayload;
        
        @SerializedName("decoded_forward_payload")
        private Object decodedForwardPayload;
        
        @SerializedName("trace_id")
        private String traceId;
    }
    
    // ========== DNS RESPONSES ==========
    
    @Data
    public static class DNSRecordsResponse {
        @SerializedName("records")
        private List<DNSRecord> records;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
    }
    
    @Data
    public static class DNSRecord {
        @SerializedName("domain")
        private String domain;
        
        @SerializedName("nft_item_address")
        private String nftItemAddress;
        
        @SerializedName("nft_item_owner")
        private String nftItemOwner;
        
        @SerializedName("dns_wallet")
        private String dnsWallet;
        
        @SerializedName("dns_next_resolver")
        private String dnsNextResolver;
        
        @SerializedName("dns_site_adnl")
        private String dnsSiteAdnl;
        
        @SerializedName("dns_storage_bag_id")
        private String dnsStorageBagId;
    }
    
    // ========== MULTISIG RESPONSES ==========
    
    @Data
    public static class MultisigResponse {
        @SerializedName("multisigs")
        private List<Multisig> multisigs;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
    }
    
    @Data
    public static class Multisig {
        @SerializedName("address")
        private String address;
        
        @SerializedName("threshold")
        private Integer threshold;
        
        @SerializedName("signers")
        private List<String> signers;
        
        @SerializedName("proposers")
        private List<String> proposers;
        
        @SerializedName("next_order_seqno")
        private String nextOrderSeqno;
        
        @SerializedName("last_transaction_lt")
        private String lastTransactionLt;
        
        @SerializedName("code_hash")
        private String codeHash;
        
        @SerializedName("data_hash")
        private String dataHash;
        
        @SerializedName("orders")
        private List<MultisigOrder> orders;
    }
    
    @Data
    public static class MultisigOrderResponse {
        @SerializedName("orders")
        private List<MultisigOrder> orders;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
    }
    
    @Data
    public static class MultisigOrder {
        @SerializedName("address")
        private String address;
        
        @SerializedName("multisig_address")
        private String multisigAddress;
        
        @SerializedName("order_seqno")
        private String orderSeqno;
        
        @SerializedName("threshold")
        private Integer threshold;
        
        @SerializedName("sent_for_execution")
        private Boolean sentForExecution;
        
        @SerializedName("signers")
        private List<String> signers;
        
        @SerializedName("approvals_num")
        private Integer approvalsNum;
        
        @SerializedName("approvals_mask")
        private String approvalsMask;
        
        @SerializedName("expiration_date")
        private Integer expirationDate;
        
        @SerializedName("order_boc")
        private String orderBoc;
        
        @SerializedName("last_transaction_lt")
        private String lastTransactionLt;
        
        @SerializedName("code_hash")
        private String codeHash;
        
        @SerializedName("data_hash")
        private String dataHash;
        
        @SerializedName("actions")
        private List<OrderAction> actions;
    }
    
    @Data
    public static class OrderAction {
        @SerializedName("send_mode")
        private Integer sendMode;
        
        @SerializedName("destination")
        private String destination;
        
        @SerializedName("value")
        private String value;
        
        @SerializedName("parsed")
        private Boolean parsed;
        
        @SerializedName("parsed_body_type")
        private String parsedBodyType;
        
        @SerializedName("parsed_body")
        private Object parsedBody;
        
        @SerializedName("body_raw")
        private List<Integer> bodyRaw;
        
        @SerializedName("error")
        private String error;
    }
    
    // ========== VESTING RESPONSES ==========
    
    @Data
    public static class VestingContractsResponse {
        @SerializedName("vesting_contracts")
        private List<VestingInfo> vestingContracts;
        
        @SerializedName("address_book")
        private Map<String, AddressBook.AddressBookEntry> addressBook;
    }
    
    @Data
    public static class VestingInfo {
        @SerializedName("address")
        private String address;
        
        @SerializedName("owner_address")
        private String ownerAddress;
        
        @SerializedName("sender_address")
        private String senderAddress;
        
        @SerializedName("start_time")
        private Integer startTime;
        
        @SerializedName("total_duration")
        private Integer totalDuration;
        
        @SerializedName("unlock_period")
        private Integer unlockPeriod;
        
        @SerializedName("cliff_duration")
        private Integer cliffDuration;
        
        @SerializedName("total_amount")
        private String totalAmount;
        
        @SerializedName("whitelist")
        private List<String> whitelist;
    }
    
    // ========== STATS RESPONSES ==========
    
    @Data
    public static class AccountBalance {
        @SerializedName("account")
        private String account;
        
        @SerializedName("balance")
        private String balance;
    }
    
    // ========== UTILS RESPONSES ==========
    
    @Data
    public static class DecodeRequest {
        @SerializedName("opcodes")
        private List<String> opcodes;
        
        @SerializedName("bodies")
        private List<String> bodies;
    }
    
    @Data
    public static class DecodeResponse {
        @SerializedName("opcodes")
        private List<String> opcodes;
        
        @SerializedName("bodies")
        private List<Map<String, Object>> bodies;
    }
    
    // ========== V2 COMPATIBILITY RESPONSES ==========
    
    @Data
    public static class V2AddressInformation {
        @SerializedName("balance")
        private String balance;
        
        @SerializedName("code")
        private String code;
        
        @SerializedName("data")
        private String data;
        
        @SerializedName("last_transaction_lt")
        private String lastTransactionLt;
        
        @SerializedName("last_transaction_hash")
        private String lastTransactionHash;
        
        @SerializedName("frozen_hash")
        private String frozenHash;
        
        @SerializedName("status")
        private String status;
    }
    
    @Data
    public static class V2WalletInformation {
        @SerializedName("balance")
        private String balance;
        
        @SerializedName("wallet_type")
        private String walletType;
        
        @SerializedName("seqno")
        private Integer seqno;
        
        @SerializedName("wallet_id")
        private Integer walletId;
        
        @SerializedName("last_transaction_lt")
        private String lastTransactionLt;
        
        @SerializedName("last_transaction_hash")
        private String lastTransactionHash;
        
        @SerializedName("status")
        private String status;
    }
    
    @Data
    public static class V2EstimateFeeRequest {
        @SerializedName("address")
        private String address;
        
        @SerializedName("body")
        private String body;
        
        @SerializedName("init_code")
        private String initCode;
        
        @SerializedName("init_data")
        private String initData;
        
        @SerializedName("ignore_chksig")
        private Boolean ignoreChksig;
    }
    
    @Data
    public static class V2EstimateFeeResult {
        @SerializedName("source_fees")
        private V2EstimatedFee sourceFees;
        
        @SerializedName("destination_fees")
        private List<V2EstimatedFee> destinationFees;
    }
    
    @Data
    public static class V2EstimatedFee {
        @SerializedName("in_fwd_fee")
        private Integer inFwdFee;
        
        @SerializedName("storage_fee")
        private Integer storageFee;
        
        @SerializedName("gas_fee")
        private Integer gasFee;
        
        @SerializedName("fwd_fee")
        private Integer fwdFee;
    }
    
    @Data
    public static class V2SendMessageRequest {
        @SerializedName("boc")
        private String boc;
    }
    
    @Data
    public static class V2SendMessageResult {
        @SerializedName("message_hash")
        private String messageHash;
        
        @SerializedName("message_hash_norm")
        private String messageHashNorm;
    }
    
    @Data
    public static class V2RunGetMethodRequest {
        @SerializedName("address")
        private String address;
        
        @SerializedName("method")
        private String method;
        
        @SerializedName("stack")
        private List<List<Object>> stack;
    }
    
    @Data
    public static class V2StackEntity {
        @SerializedName("type")
        private String type;
        
        @SerializedName("value")
        private Object value;
    }
}
