package org.ton.java.smartcontract.token.nft;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.smartcontract.types.Royalty;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.tonlib.types.TvmStackEntrySlice;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class NftUtils {


    public static final int SNAKE_DATA_PREFIX = 0x00;
    public static final int CHUNK_DATA_PREFIX = 0x01;
    public static final int ONCHAIN_CONTENT_PREFIX = 0x00;
    public static final int OFFCHAIN_CONTENT_PREFIX = 0x01;

    /**
     * @param uri String
     * @return byte[]
     */
    public static byte[] serializeUri(String uri) {
        try {
            return URLEncoder.encode(uri, StandardCharsets.UTF_8).getBytes();
        } catch (Exception e) {
            throw new Error("Cannot serialize URI " + uri);
        }
    }

    /**
     * @param uri
     * @return String
     */
    static String parseUri(String uri) {
        return URLDecoder.decode(uri, StandardCharsets.UTF_8);
    }

    /**
     * @param uri String
     * @return Cell
     */
    public static Cell createOffChainUriCell(String uri) {
        return CellBuilder.beginCell()
                .storeUint(OFFCHAIN_CONTENT_PREFIX, 8)
                .storeSnakeString(uri)
                .endCell();
    }

    /**
     * @param cell Cell
     * @return String
     */
    public static String parseOffChainUriCell(Cell cell) {
        if ((cell.getBits().toByteArray()[0] & 0xFF) != OFFCHAIN_CONTENT_PREFIX) {
            throw new Error("not OFFCHAIN_CONTENT_PREFIX");
        }

        return parseUri(CellSlice.beginParse(cell).skipBits(8).loadSnakeString());
    }

    public static String parseOnChainUriCell(Cell cell) {
        return parseUri(CellSlice.beginParse(cell).loadSnakeString());
    }

    /**
     * TODO onchain content
     * The first byte is 0x00 and the rest is key/value dictionary.
     * Key is sha256 hash of string.
     * Value is data encoded as described in "Data serialization" paragraph.
     *
     * @param name        String name of Jetton
     * @param description String description of Jetton
     * @return Cell
     */
    public static Cell createOnchainDataCell(String name, String description) { // https://github.com/ton-blockchain/TIPs/issues/64
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(ONCHAIN_CONTENT_PREFIX, 8);
        cell.storeBytes(name.getBytes(StandardCharsets.UTF_8));
        cell.storeString(description);
        return cell.endCell();
    }

    /**
     * @param cell Cell
     * @return Address|null
     */
    public static Address parseAddress(Cell cell) {

        return CellSlice.beginParse(cell).loadAddress();
    }

    /**
     * @param tonlib  Tonlib
     * @param address String
     * @return Royalty
     */
    public static Royalty getRoyaltyParams(Tonlib tonlib, Address address) {
        RunResult result = tonlib.runMethod(address, "royalty_params");

        TvmStackEntryNumber royaltyFactorNumber = (TvmStackEntryNumber) result.getStack().get(0);
        BigInteger royaltyFactor = royaltyFactorNumber.getNumber();

        TvmStackEntryNumber royaltyBaseNumber = (TvmStackEntryNumber) result.getStack().get(1);
        BigInteger royaltyBase = royaltyBaseNumber.getNumber();

        double royalty = royaltyFactor.divide(royaltyBase).doubleValue();
        TvmStackEntrySlice royaltyAddressCell = (TvmStackEntrySlice) result.getStack().get(2);
        Address royaltyAddress = NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(royaltyAddressCell.getSlice().getBytes())).endCell());

        return Royalty.builder()
                .royaltyFactor(royaltyFactor)
                .royaltyBase(royaltyBase)
                .royalty(royalty)
                .royaltyAddress(royaltyAddress)
                .build();
    }
}
