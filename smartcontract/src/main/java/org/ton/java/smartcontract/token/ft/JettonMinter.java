package org.ton.java.smartcontract.token.ft;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.JettonMinterData;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryCell;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.tonlib.types.TvmStackEntrySlice;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;

import static java.util.Objects.isNull;

@Builder
@Getter
public class JettonMinter implements Contract {
    TweetNaclFast.Signature.KeyPair keyPair;
    Address adminAddress;
    Cell content;
    String jettonWalletCodeHex;

    String code;

    public static class JettonMinterBuilder {
    }

    public static JettonMinterBuilder builder() {
        return new CustomJettonMinterBuilder();
    }

    private static class CustomJettonMinterBuilder extends JettonMinterBuilder {
        @Override
        public JettonMinter build() {
            if (isNull(super.keyPair)) {
                super.keyPair = Utils.generateSignatureKeyPair();
            }

            return super.build();
        }
    }

    private Tonlib tonlib;
    private long wc;

    @Override
    public Tonlib getTonlib() {
        return tonlib;
    }

    @Override
    public long getWorkchain() {
        return wc;
    }

    public String getName() {
        return "jettonMinter";
    }

    /**
     * @return Cell cell - contains jetton data cell
     */
    @Override
    public Cell createDataCell() {
        return CellBuilder.beginCell()
                .storeCoins(BigInteger.ZERO)
                .storeAddress(adminAddress)
                .storeRef(content)
                .storeRef(CellBuilder.beginCell().fromBoc(jettonWalletCodeHex).endCell())
                .endCell();
    }

    @Override
    public Cell createCodeCell() {
        if (StringUtils.isNotEmpty(code)) {
            System.out.println("Using custom JettonMinter");
            return CellBuilder.beginCell().
                    fromBoc(code).
                    endCell();
        }
        return CellBuilder.beginCell().
                fromBoc(WalletCodes.jettonMinter.getValue()).
                endCell();
    }

    /**
     * @param queryId          long
     * @param destination      Address
     * @param amount           BigInteger
     * @param jettonAmount     BigInteger
     * @param fromAddress      Address
     * @param responseAddress  Address
     * @param forwardTonAmount BigInteger
     * @return Cell
     */
    public static Cell createMintBody(long queryId, Address destination, BigInteger amount,
                                      BigInteger jettonAmount, Address fromAddress, Address
                                              responseAddress, BigInteger forwardTonAmount,
                                      Cell forwardPayload) {
        return CellBuilder.beginCell()
                .storeUint(21, 32) // OP mint
                .storeUint(queryId, 64)   // query_id, default 0
                .storeAddress(destination)
                .storeCoins(amount)
                .storeRef(CellBuilder.beginCell()
                        .storeUint(0x178d4519, 32) // internal_transfer op
                        .storeUint(queryId, 64) // default 0
                        .storeCoins(jettonAmount)
                        .storeAddress(fromAddress)     // from_address
                        .storeAddress(responseAddress) // response_address
                        .storeCoins(forwardTonAmount)  // forward_ton_amount
                        .storeBit(false) // store payload in the same cell
                        .storeCell(forwardPayload) // forward payload
                        .endCell())
                .endCell();
    }

    /**
     * @param queryId         long
     * @param newAdminAddress Address
     * @return Cell
     */
    public Cell createChangeAdminBody(long queryId, Address newAdminAddress) {
        if (isNull(newAdminAddress)) {
            throw new Error("Specify newAdminAddress");
        }

        return CellBuilder.beginCell()
                .storeUint(3, 32) // OP
                .storeUint(queryId, 64) // query_id
                .storeAddress(newAdminAddress)
                .endCell();
    }

    /**
     * @param jettonContentUri: String
     * @param queryId           long
     * @return Cell
     */
    public Cell createEditContentBody(String jettonContentUri, long queryId) {
        return CellBuilder.beginCell()
                .storeUint(4, 32) // OP change content
                .storeUint(queryId, 64) // query_id
                .storeRef(NftUtils.createOffChainUriCell(jettonContentUri))
                .endCell();
    }

    /**
     * @return JettonData
     */
    public JettonMinterData getJettonData(Tonlib tonlib) {
        RunResult result = tonlib.runMethod(getAddress(), "get_jetton_data"); // minter

        if (result.getExit_code() != 0) {
            throw new Error("method get_jetton_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber totalSupplyNumber = (TvmStackEntryNumber) result.getStack().get(0);
        BigInteger totalSupply = totalSupplyNumber.getNumber();

        boolean isMutable = ((TvmStackEntryNumber) result.getStack().get(1)).getNumber().longValue() == -1;

        TvmStackEntrySlice adminAddr = (TvmStackEntrySlice) result.getStack().get(2);
        Address adminAddress = NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(adminAddr.getSlice().getBytes())).endCell());

        TvmStackEntryCell jettonContent = (TvmStackEntryCell) result.getStack().get(3);
        Cell jettonContentCell = CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(jettonContent.getCell().getBytes())).endCell();
        String jettonContentUri = null;
        try {
            jettonContentUri = NftUtils.parseOffChainUriCell(jettonContentCell);
        } catch (Exception e) {
            //todo
        }

        TvmStackEntryCell contentC = (TvmStackEntryCell) result.getStack().get(4);
        Cell jettonWalletCode = CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(contentC.getCell().getBytes())).endCell();

        return JettonMinterData.builder()
                .totalSupply(totalSupply)
                .isMutable(isMutable)
                .adminAddress(adminAddress)
                .jettonContentCell(jettonContentCell)
                .jettonContentUri(jettonContentUri)
                .jettonWalletCode(jettonWalletCode)
                .build();
    }

    public BigInteger getTotalSupply(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_jetton_data");

        if (result.getExit_code() != 0) {
            throw new Error("method get_jetton_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber totalSupplyNumber = (TvmStackEntryNumber) result.getStack().get(0);
        return totalSupplyNumber.getNumber();
    }

    /**
     * @param ownerAddress Address
     * @return Address user_jetton_wallet_address
     */
    public JettonWallet getJettonWallet(Address ownerAddress) {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeAddress(ownerAddress);

        Deque<String> stack = new ArrayDeque<>();

        stack.offer("[slice, " + cell.endCell().toHex(true) + "]");

        RunResult result = tonlib.runMethod(getAddress(), "get_wallet_address", stack);

        if (result.getExit_code() != 0) {
            throw new Error("method get_wallet_address, returned an exit code " + result.getExit_code());
        }

        TvmStackEntrySlice addr = (TvmStackEntrySlice) result.getStack().get(0);
        Address jettonWalletAddress = NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(addr.getSlice().getBytes())).endCell());

        return JettonWallet.builder()
                .tonlib(tonlib)
                .address(jettonWalletAddress)
                .build();
    }
}