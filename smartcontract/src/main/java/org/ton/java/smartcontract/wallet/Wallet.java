package org.ton.java.smartcontract.wallet;

import org.ton.java.smartcontract.dns.DnsCollection;
import org.ton.java.smartcontract.dns.DnsItem;
import org.ton.java.smartcontract.lockup.LockupWalletV1;
import org.ton.java.smartcontract.token.ft.JettonMinter;
import org.ton.java.smartcontract.token.ft.JettonWallet;
import org.ton.java.smartcontract.token.nft.NftCollection;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.v1.SimpleWalletContractR1;
import org.ton.java.smartcontract.wallet.v1.SimpleWalletContractR2;
import org.ton.java.smartcontract.wallet.v1.SimpleWalletContractR3;
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

        Contract result = null;

        switch (walletVersion) {
            case simpleR1:
                result = new SimpleWalletContractR1(options);
                break;
            case simpleR2:
                result = new SimpleWalletContractR2(options);
                break;
            case simpleR3:
                result = new SimpleWalletContractR3(options);
                break;
            case v2R1:
                result = new WalletV2ContractR1(options);
                break;
            case v2R2:
                result = new WalletV2ContractR2(options);
                break;
            case v3R1:
                result = new WalletV3ContractR1(options);
                break;
            case v3R2:
                result = new WalletV3ContractR2(options);
                break;
            case v4R2:
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
        }
        return (T) result;
    }
}
