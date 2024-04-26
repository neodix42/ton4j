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

        Contract result;
        switch (walletVersion) {
            case V1R1:
                result = new WalletV1ContractR1(options);
                break;
            case V1R2:
                result = new WalletV1ContractR2(options);
                break;
            case V1R3:
                result = new WalletV1ContractR3(options);
                break;
            case V2R1:
                result = new WalletV2ContractR1(options);
                break;
            case V2R2:
                result = new WalletV2ContractR2(options);
                break;
            case V3R1:
                result = new WalletV3ContractR1(options);
                break;
            case V3R2:
                result = new WalletV3ContractR2(options);
                break;
            case V4R2:
                result = new WalletV4ContractR2(options);
                break;
            case lockup:
                result = new LockupWalletV1(options);
                break;
            case dnsCollection:
                result = new DnsCollection(options);
                break;
            case dnsItem:
                result = new DnsItem(options);
                break;
            case jettonMinter:
                result = new JettonMinter(options);
                break;
            case jettonWallet:
                result = new JettonWallet(options);
                break;
            case nftCollection:
                result = new NftCollection(options);
                break;
            case payments:
                result = new PaymentChannel(options);
                break;
            case highload:
                result = new HighloadWallet(options);
                break;
            case highloadV3:
                result = new HighloadWalletV3(options);
                break;
            case multisig:
                result = new MultisigWallet(options);
                break;
            case master:
                throw new Error("not implemented");
            case config:
                throw new Error("not implemented");
            case unidentified:
                throw new Error("not implemented");
            default:
                throw new IllegalArgumentException("Unknown wallet version: " + walletVersion);
        }

        return (T) result;
    }
}
