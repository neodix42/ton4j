package org.ton.java.smartcontract.wallet;

import org.ton.java.smartcontract.dns.DnsCollection;
import org.ton.java.smartcontract.dns.DnsItem;
import org.ton.java.smartcontract.highload.HighloadWallet;
import org.ton.java.smartcontract.highload.HighloadWalletV3;
import org.ton.java.smartcontract.lockup.LockupWalletV1;
import org.ton.java.smartcontract.multisig.MultisigWallet;
import org.ton.java.smartcontract.payments.PaymentChannel;
import org.ton.java.smartcontract.token.ft.JettonMinter;
import org.ton.java.smartcontract.token.ft.JettonWallet;
import org.ton.java.smartcontract.token.nft.NftCollection;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.v1.WalletV1ContractR1;
import org.ton.java.smartcontract.wallet.v1.WalletV1ContractR2;
import org.ton.java.smartcontract.wallet.v1.WalletV1ContractR3;
import org.ton.java.smartcontract.wallet.v2.WalletV2ContractR1;
import org.ton.java.smartcontract.wallet.v2.WalletV2ContractR2;
import org.ton.java.smartcontract.wallet.v3.WalletV3ContractR1;
import org.ton.java.smartcontract.wallet.v3.WalletV3ContractR2;
import org.ton.java.smartcontract.wallet.v4.WalletV4ContractR2;

public class Wallet {

    Options options;
    WalletVersion walletVersion;

    public Wallet(WalletVersion walletVersion, Options options) {
        this.walletVersion = walletVersion;
        this.options = options;
    }

    public Wallet(WalletVersion walletVersion) {
        this.walletVersion = walletVersion;
        this.options = Options.builder().build();
    }

    public <T extends Contract> T create() {

        Contract result = switch (walletVersion) {
            case V1R1 -> new WalletV1ContractR1(options);
            case V1R2 -> new WalletV1ContractR2(options);
            case V1R3 -> new WalletV1ContractR3(options);
            case V2R1 -> new WalletV2ContractR1(options);
            case V2R2 -> new WalletV2ContractR2(options);
            case V3R1 -> new WalletV3ContractR1(options);
            case V3R2 -> new WalletV3ContractR2(options);
            case V4R2 -> new WalletV4ContractR2(options);
            case lockup -> new LockupWalletV1(options);
            case dnsCollection -> new DnsCollection(options);
            case dnsItem -> new DnsItem(options);
            case jettonMinter -> new JettonMinter(options);
            case jettonWallet -> new JettonWallet(options);
            case nftCollection -> new NftCollection(options);
            case payments -> new PaymentChannel(options);
            case highload -> new HighloadWallet(options);
            case highloadV3 -> new HighloadWalletV3(options);
            case multisig -> new MultisigWallet(options);
            case master -> throw new Error("not implemented");
            case config -> throw new Error("not implemented");
            case unidentified -> throw new Error("not implemented");
        };

        return (T) result;
    }
}
