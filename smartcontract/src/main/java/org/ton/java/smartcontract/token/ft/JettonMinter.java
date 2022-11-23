package org.ton.java.smartcontract.token.ft;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.token.nft.NftUtils;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.JettonMinterData;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryCell;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class JettonMinter implements Contract {

    public static final String JETTON_MINTER_CODE_HEX = "B5EE9C7241020B010001ED000114FF00F4A413F4BCF2C80B0102016202030202CC040502037A60090A03EFD9910E38048ADF068698180B8D848ADF07D201800E98FE99FF6A2687D007D206A6A18400AA9385D47181A9AA8AAE382F9702480FD207D006A18106840306B90FD001812881A28217804502A906428027D012C678B666664F6AA7041083DEECBEF29385D71811A92E001F1811802600271812F82C207F97840607080093DFC142201B82A1009AA0A01E428027D012C678B00E78B666491646580897A007A00658064907C80383A6465816503E5FFE4E83BC00C646582AC678B28027D0109E5B589666664B8FD80400FE3603FA00FA40F82854120870542013541403C85004FA0258CF1601CF16CCC922C8CB0112F400F400CB00C9F9007074C8CB02CA07CBFFC9D05008C705F2E04A12A1035024C85004FA0258CF16CCCCC9ED5401FA403020D70B01C3008E1F8210D53276DB708010C8CB055003CF1622FA0212CB6ACB1FCB3FC98042FB00915BE200303515C705F2E049FA403059C85004FA0258CF16CCCCC9ED54002E5143C705F2E049D43001C85004FA0258CF16CCCCC9ED54007DADBCF6A2687D007D206A6A183618FC1400B82A1009AA0A01E428027D012C678B00E78B666491646580897A007A00658064FC80383A6465816503E5FFE4E840001FAF16F6A2687D007D206A6A183FAA904051007F09";

    Options options;
    Address address;

    /**
     * @param options Options
     */
    public JettonMinter(Options options) {
        this.options = options;
        this.options.wc = 0;

        if (nonNull(options.address)) {
            this.address = Address.of(options.address);
        }
        if (isNull(options.code)) {
            options.code = Cell.fromBoc(JETTON_MINTER_CODE_HEX);
        }
    }

    public String getName() {
        return "jettonMinter";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public Address getAddress() {
        if (address == null) {
            return (createStateInit()).address;
        }
        return address;
    }

    /**
     * @return Cell cell - contains jetton data cell
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeCoins(BigInteger.ZERO);
        cell.storeAddress(options.adminAddress);
        cell.storeRef(NftUtils.createOffchainUriCell(options.jettonContentUri));
        cell.storeRef(Cell.fromBoc(options.jettonWalletCodeHex));
        return cell.endCell();
    }

    /**
     * @param queryId      long
     * @param destination  Address
     * @param amount       BigInteger
     * @param jettonAmount BigInteger
     * @return Cell
     */
    public Cell createMintBody(long queryId, Address destination, BigInteger amount, BigInteger jettonAmount) {

        return createMintBody(queryId, destination, amount, jettonAmount, null, null, BigInteger.ZERO);
    }

    /**
     * @param queryId         long
     * @param destination     Address
     * @param amount          BigInteger
     * @param jettonAmount    BigInteger
     * @param fromAddress     Address
     * @param responseAddress Address
     * @param forwardAmount   BigInteger
     * @return Cell
     */
    public Cell createMintBody(long queryId, Address destination, BigInteger amount, BigInteger jettonAmount, Address fromAddress, Address responseAddress, BigInteger forwardAmount) {
        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(21, 32); // OP mint
        body.storeUint(queryId, 64); // query_id, default 0
        body.storeAddress(destination);
        body.storeCoins(amount);

        CellBuilder transferBody = CellBuilder.beginCell(); // internal transfer
        transferBody.storeUint(0x178d4519, 32); // internal_transfer op
        transferBody.storeUint(queryId, 64); // default 0
        transferBody.storeCoins(jettonAmount);
        transferBody.storeAddress(fromAddress); // from_address
        transferBody.storeAddress(responseAddress); // response_address
        transferBody.storeCoins(forwardAmount); // forward_amount
        transferBody.storeBit(false); // forward_payload in this slice, not separate cell

        body.storeRef(transferBody);

        return body;
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

        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(3, 32); // OP
        body.storeUint(queryId, 64); // query_id
        body.storeAddress(newAdminAddress);
        return body;
    }

    /**
     * @param jettonContentUri: String
     * @param queryId           long
     * @return Cell
     */
    public Cell createEditContentBody(String jettonContentUri, long queryId) {
        CellBuilder body = CellBuilder.beginCell();
        body.storeUint(4, 32); // OP change content
        body.storeUint(queryId, 64); // query_id
        body.storeRef(NftUtils.createOffchainUriCell(jettonContentUri));
        return body;
    }

    /**
     * @return JettonData
     */
    public JettonMinterData getJettonData(Tonlib tonlib) {
        Address myAddress = this.getAddress();
        RunResult result = tonlib.runMethod(myAddress, "get_jetton_data"); //minter

        if (result.getExit_code() != 0) {
            throw new Error("method get_nft_data, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryNumber totalSupplyNumber = (TvmStackEntryNumber) result.getStack().get(0);
        BigInteger totalSupply = totalSupplyNumber.getNumber();

        System.out.println("minter totalSupply: " + Utils.formatNanoValue(totalSupply));

        boolean isMutable = ((TvmStackEntryNumber) result.getStack().get(1)).getNumber().longValue() == -1;

        TvmStackEntryCell adminAddr = (TvmStackEntryCell) result.getStack().get(2);
        Address adminAddress = NftUtils.parseAddress(CellBuilder.fromBoc(Utils.base64ToBytes(adminAddr.getCell().getBytes())));

        TvmStackEntryCell jettonContent = (TvmStackEntryCell) result.getStack().get(3);
        Cell jettonContentCell = CellBuilder.fromBoc(Utils.base64ToBytes(jettonContent.getCell().getBytes()));
        String jettonContentUri = null;
        try {
            jettonContentUri = NftUtils.parseOffchainUriCell(jettonContentCell);
        } catch (Exception e) {
            //todo
        }

        TvmStackEntryCell contentC = (TvmStackEntryCell) result.getStack().get(4);
        Cell jettonWalletCode = CellBuilder.fromBoc(Utils.base64ToBytes(contentC.getCell().getBytes()));

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
     * @param tonlib       Tonlib
     * @param ownerAddress Address
     * @return Address user_jetton_wallet_address
     */
    public Address getJettonWalletAddress(Tonlib tonlib, Address ownerAddress) {
        Address myAddress = this.getAddress();
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeAddress(ownerAddress);

        Deque<String> stack = new ArrayDeque<>();

        stack.offer("[slice, " + cell.endCell().toHex(true) + "]");

        RunResult result = tonlib.runMethod(myAddress, "get_wallet_address", stack);

        if (result.getExit_code() != 0) {
            throw new Error("method get_wallet_address, returned an exit code " + result.getExit_code());
        }

        TvmStackEntryCell addr = (TvmStackEntryCell) result.getStack().get(0);
        return NftUtils.parseAddress(CellBuilder.fromBoc(Utils.base64ToBytes(addr.getCell().getBytes())));
    }

    public void deploy(Tonlib tonlib, WalletContract adminWallet, BigInteger walletMsgValue, TweetNaclFast.Signature.KeyPair keyPair) {
        long seqno = adminWallet.getSeqno(tonlib);

        ExternalMessage extMsg = adminWallet.createTransferMessage(
                keyPair.getSecretKey(),
                this.getAddress(),
                walletMsgValue,
                seqno,
                (Cell) null, // body
                (byte) 3, //send mode
                false, //dummy signature
                this.createStateInit().stateInit);

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }

    public void mint(Tonlib tonlib, WalletContract adminWallet, Address destination, BigInteger walletMsgValue, BigInteger mintMsgValue, BigInteger jettonToMintAmount, TweetNaclFast.Signature.KeyPair keyPair) {

        long seqno = adminWallet.getSeqno(tonlib);

        ExternalMessage extMsg = adminWallet.createTransferMessage(
                keyPair.getSecretKey(),
                this.getAddress(),
                walletMsgValue,
                seqno,
                this.createMintBody(0, destination, mintMsgValue, jettonToMintAmount));

        tonlib.sendRawMessage(Utils.bytesToBase64(extMsg.message.toBoc(false)));
    }
}