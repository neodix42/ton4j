package org.ton.java.smartcontract.integrationtests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.ton.java.utils.Utils.bytesToHex;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.mnemonic.Mnemonic;
import org.ton.java.mnemonic.Pair;
import org.ton.java.smartcontract.token.ft.JettonMinter;
import org.ton.java.smartcontract.token.ft.JettonMinterStableCoin;
import org.ton.java.smartcontract.token.ft.JettonWallet;
import org.ton.java.smartcontract.token.ft.JettonWalletStableCoin;
import org.ton.java.smartcontract.types.Destination;
import org.ton.java.smartcontract.types.HighloadQueryId;
import org.ton.java.smartcontract.types.WalletV5Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.v4.WalletV4R2;
import org.ton.java.smartcontract.wallet.v5.WalletV5;
import org.ton.java.tlb.types.Account;
import org.ton.java.tlb.types.AccountStateActive;
import org.ton.java.tlb.types.CurrencyCollection;
import org.ton.java.tlb.types.InternalMessageInfo;
import org.ton.java.tlb.types.Message;
import org.ton.java.tlb.types.MsgAddressInt;
import org.ton.java.tlb.types.MsgAddressIntStd;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;
import org.ton.schema.gasless.GaslessConfig;
import org.ton.schema.gasless.SignRawMessage;
import org.ton.schema.gasless.SignRawParams;
import org.ton.tonapi.sync.Tonapi;

import java.util.List;
import org.ton.tonapi.sync.methods.GaslessMethod;


@Slf4j
public class GaslessTransactionTest {

    private static final String API_KEY = "AGAGTULGORVJQBAAAAAELZDTJD5KSFEUVFQOI6WCBOZB5SVYSTWUMAKTU6TOXBXZF5M6ZPQ";

    private static final Address RECIPIENT = Address.of("UQB8ANV_ynITQr1qHXADHDKYUAQ9VFcCRDZB7h4aPuPKuFtm");
    private static final Address USDT_MASTER = Address.of("EQCxE6mUtQJKFnGfaROTKOt1lZbDiiX1kCixRv7Nw2Id_sDs");

    // TON API client initialization
    private final Tonapi tonApiClient = new Tonapi(API_KEY, true, 10);

    static Tonlib tonlib;

    private Address getRelayAddress(GaslessMethod gaslessMethod) {
        GaslessConfig config = gaslessMethod.getConfig();
        return Address.of(config.getRelayAddress());
    }

    @Test
    public void testGaslessTransaction() throws Exception {
        tonlib = Tonlib.builder()
            .testnet(false)
            .ignoreCache(false)
            .build();

        GaslessMethod gaslessMethod = new GaslessMethod(tonApiClient);

        List<String> srcSeed = Arrays.asList();

        Pair keyPairSrc = Mnemonic.toKeyPair(srcSeed);

        log.info("public-key {}", Utils.bytesToHex(keyPairSrc.getPublicKey()));
        log.info("secret-key {}", Utils.bytesToHex(keyPairSrc.getSecretKey()));

        TweetNaclFast.Signature.KeyPair keyPairSigSrc = TweetNaclFast.Signature.keyPair_fromSeed(keyPairSrc.getSecretKey());

        long walletId = 2147483409L;

        // connect to a source wallet
        WalletV5 srcWallet = WalletV5.builder()
            .tonlib(tonlib)
            .isSigAuthAllowed(true)
            .walletId(walletId)
            .keyPair(keyPairSigSrc)
            .build();

        Address srcWalletAddress = srcWallet.getAddress();

        String nonBounceableAddress = srcWalletAddress.toNonBounceable();
        String bounceableAddress = srcWalletAddress.toBounceable();
        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("bounceable address {}", bounceableAddress);
        log.info("pub-key {}", bytesToHex(srcWallet.getKeyPair().getPublicKey()));
        log.info("prv-key {}", bytesToHex(srcWallet.getKeyPair().getSecretKey()));
        log.info("Wallet {} balance: {}", srcWallet.getName(), Utils.formatNanoValue(srcWallet.getBalance()));

        log.info("walletId {}", srcWallet.getWalletId());
        log.info("publicKey {}", bytesToHex(srcWallet.getPublicKey()));
        log.info("isSignatureAuthAllowed {}", srcWallet.getIsSignatureAuthAllowed());

        String status = tonlib.getAccountStatus(Address.of(bounceableAddress));
        log.info("account status {}", status);

        BigInteger balance = tonlib.getAccountBalance(Address.of(bounceableAddress));
        log.info("account balance {}", Utils.formatNanoValue(balance));

        Address relay = getRelayAddress(gaslessMethod);

        JettonMinterStableCoin usdtMasterWallet = JettonMinterStableCoin.builder()
            .tonlib(tonlib)
            .customAddress(USDT_MASTER)
            .build();

        log.info("USDT total supply: {}", Utils.formatJettonValue(usdtMasterWallet.getTotalSupply(), 6, 2));

        // get my JettonWallet the one that holds my jettons (USDT) tokens
        JettonWalletStableCoin srcJettonWallet = usdtMasterWallet.getJettonWallet(srcWalletAddress);
        log.info("my jettonWallet balance: {}", Utils.formatJettonValue(srcJettonWallet.getBalance(), 6, 2));

        Cell bodyCell = srcWallet.createBulkTransfer(
                Collections.singletonList(
                    Destination.builder()
                        .address(RECIPIENT.toString())
                        .bounce(true)
                        .body(JettonWalletStableCoin.createTransferBody(
                            0,
                            BigInteger.valueOf(1_000_000),              // 1 USDT
                            RECIPIENT,                            // recipient
                            relay,                                 // response address
                            BigInteger.ONE,                       // forward amount
                            MsgUtils.createTextMessageBody("test gasless") // comment
                        ))
                        .build()))
            .toCell();

        WalletV5Config walletV5Config = WalletV5Config.builder()
            .walletId(walletId)
            .seqno(srcWallet.getSeqno())
            .body(bodyCell)
            .build();

        String msg = srcWallet.createInternalTransferBody(walletV5Config).toHex();

        log.info("Prepared message: {}", msg);

        String pubKey = bytesToHex(keyPairSigSrc.getPublicKey());

        // we send a single message containing a transfer from our wallet to a desired destination.
        // as a result of estimation, TonAPI returns a list of messages that we need to sign.
        // the first message is a fee transfer to the relay address, the second message is our original transfer.
        SignRawParams signRawParams = gaslessMethod.estimateGasPrice(
            USDT_MASTER.toString(),
            srcWalletAddress.toString(),
            pubKey,
            Arrays.asList(msg)
        );

        // signRawParams is the same structure as signRawParams in tonconnect.
        List<Destination> msgs = new ArrayList<>();

        for (SignRawMessage signRawMessage : signRawParams.getMessages()) {

//            InternalMessageInfo internalMessageInfo = InternalMessageInfo.builder()
//                .iHRDisabled(true)
//                .bounce(false)
//                .bounced(false)
//                .value(CurrencyCollection.builder()
//                    .coins(Utils.toNano(1))
//                    .build())
//                .srcAddr(srcWallet.getAddressIntStd())
//                .dstAddr(MsgAddressIntStd.builder()
//                    .workchainId(RECIPIENT.wc)
//                    .address(RECIPIENT.toBigInteger())
//                    .build())
//                .build();


            Cell msgCell = Cell.fromBoc(signRawMessage.getPayload());
            BigInteger msgAmount = signRawMessage.getAmount();
            Destination rawMessage = Destination.builder()
                    .address(signRawMessage.getAddress())
                    .amount(msgAmount)
                    .body(msgCell)
                    .build();
            msgs.add(rawMessage);
        }

        WalletV5Config conf = WalletV5Config.builder()
                .walletId(42)
                .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
                .body(srcWallet.createBulkTransfer(msgs).toCell())
                .build();

        Cell message = srcWallet.prepareExternalMsg(conf).toCell();
        String msgBoc = message.toBase64();

        boolean isTxSuccess = gaslessMethod.send(pubKey, msgBoc);

        assertThat(isTxSuccess).isEqualTo(true);

        BigInteger balanceOfDestinationWallet = tonlib.getAccountBalance(RECIPIENT);
        log.info("balanceOfDestinationWallet in toncoins: {}", balanceOfDestinationWallet);

        JettonWalletStableCoin dstJettonWallet = usdtMasterWallet.getJettonWallet(RECIPIENT);
        BigInteger dstJettonWalletBalance = dstJettonWallet.getBalance();
        log.info("dstJettonWallet balance in jettons: {}", Utils.formatJettonValue(dstJettonWalletBalance, 6, 2));

    }
}
