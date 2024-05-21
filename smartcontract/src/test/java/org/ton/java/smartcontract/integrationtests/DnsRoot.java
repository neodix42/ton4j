package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

import static java.util.Objects.isNull;

/**
 * Address address1; address of ".ton" dns resolver smart contract in basechain
 * Address address2; address of ".t.me" dns resolver smart contract in basechain
 * Address address3; address of "www.ton" dns resolver smart contract in basechain
 */
@Builder
@Getter
public class DnsRoot implements Contract {

    public static final String DNS_ROOT_CODE_HEX = "B5EE9C7241020801000164000114FF00F4A413F4BCF2C80B0102016202030202CD040502FBA1C619DBF66041AE92F152118001E5C08C41AE140F800043AE92418010A4416128BE06F0DBC0432A05A60E6205BDDA89A1F481F481F48060E04191166E8DEDD19E2D960F166EEEEEF19E2D960F93A049847F1C484DAE3A7E038E0B1C326ABE070401752791961EB19E2D920322F122E1C54C7003B663C06122B7C4E1911306070005D2F81C00936483001258C2040FA201938083001658C20407D200CB8083001A58C204064200A38083001E58C20404B2007B8083002258C204032200538083002650C20191EB83002A4E00C9D781E9C60006746F6E00E6CF16CB07C9D023C21F8E2425D71D1F01C7058E193031328200BA93C8CB0F58CF16C90191789170E2A61801DB31E031923031E27020C88B26D658CF16CB078B1748CF16CB07C9D002C2278E2103D71D2701C7058E168200BA93C8CB0F58CF16C90191789170E2A62001DB31E05B925F04E2706DD3153191";
    TweetNaclFast.Signature.KeyPair keyPair;

    Address address1; // address of ".ton" dns resolver smart contract in basechain
    Address address2; // address of ".t.me" dns resolver smart contract in basechain
    Address address3; // address of "www.ton" dns resolver smart contract in basechain

    public static class DnsRootBuilder {
        DnsRootBuilder() {
            if (isNull(keyPair)) {
                keyPair = Utils.generateSignatureKeyPair();
            }
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
        return "dnsRoot";
    }

    @Override
    public Cell createDataCell() {
        return CellBuilder.beginCell()
                .storeAddress(address1)
                .storeAddress(address2)
                .storeAddress(address3)
                .endCell();
    }

    @Override
    public Cell createCodeCell() {
        return CellBuilder.beginCell().fromBoc(DNS_ROOT_CODE_HEX).endCell();
    }

}
