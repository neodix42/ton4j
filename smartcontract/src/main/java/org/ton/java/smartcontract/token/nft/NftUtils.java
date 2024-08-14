package org.ton.java.smartcontract.token.nft;

import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;
import org.ton.java.smartcontract.types.Royalty;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.tonlib.types.TvmStackEntrySlice;
import org.ton.java.utils.Utils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Log
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
            return URLEncoder.encode(uri, String.valueOf(StandardCharsets.UTF_8)).getBytes();
        } catch (Exception e) {
            throw new Error("Cannot serialize URI " + uri);
        }
    }

    /**
     * @param uri
     * @return String
     */
    static String parseUri(String uri) {
        try {
            return URLDecoder.decode(uri, String.valueOf(StandardCharsets.UTF_8));
        } catch (UnsupportedEncodingException e) {
            log.info(e.getMessage());
            return null;
        }
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

        if ((cell.getBits().toByteArray()[0] & 0xFF) != ONCHAIN_CONTENT_PREFIX) {
            throw new Error("not ONCHAIN_CONTENT_PREFIX");
        }

        CellSlice cs = CellSlice.beginParse(cell);
        cs.skipBits(8);

        TonHashMapE loadedDict = cs
                .loadDictE(256,
                        k -> k.readUint(256),
                        v -> CellSlice.beginParse(v).loadSnakeString()
                );
        BigInteger key = new BigInteger(Utils.sha256("uri".getBytes()), 16);
        String uri = loadedDict.elements.get(key).toString();
        return StringUtils.trim(uri);
    }

    /**
     * The first byte is 0x00 and the rest is key/value dictionary.
     * Key is sha256 hash of string.
     * Value is data encoded as described in "Data serialization" paragraph.
     *
     * @param uri
     * @return Cell
     */
    public static Cell createOnChainDataCell(String uri, Long decimals) {
        // https://github.com/ton-blockchain/TIPs/issues/64
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(ONCHAIN_CONTENT_PREFIX, 8);

        int keySizeX = 256;
        TonHashMapE x = new TonHashMapE(keySizeX);

        BigInteger uriKey = new BigInteger(Utils.sha256("uri".getBytes()), 16);
        x.elements.put(uriKey, CellBuilder.beginCell().storeSnakeString(uri).endCell());
        BigInteger decimalsKey = new BigInteger(Utils.sha256("decimals".getBytes()), 16);

        x.elements.put(uriKey, CellBuilder.beginCell().storeSnakeString(uri).endCell());
        x.elements.put(decimalsKey, CellBuilder.beginCell().storeString(decimals.toString()).endCell());

        Cell cellDict = x.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, keySizeX).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell((Cell) v).endCell()
        );

        cell.storeDict(cellDict);
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
