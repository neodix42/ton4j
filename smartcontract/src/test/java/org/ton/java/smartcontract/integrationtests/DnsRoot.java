package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class DnsRoot implements Contract {

    public static final String DNS_ROOT_CODE_HEX = "B5EE9C7241020801000164000114FF00F4A413F4BCF2C80B0102016202030202CD040502FBA1C619DBF66041AE92F152118001E5C08C41AE140F800043AE92418010A4416128BE06F0DBC0432A05A60E6205BDDA89A1F481F481F48060E04191166E8DEDD19E2D960F166EEEEEF19E2D960F93A049847F1C484DAE3A7E038E0B1C326ABE070401752791961EB19E2D920322F122E1C54C7003B663C06122B7C4E1911306070005D2F81C00936483001258C2040FA201938083001658C20407D200CB8083001A58C204064200A38083001E58C20404B2007B8083002258C204032200538083002650C20191EB83002A4E00C9D781E9C60006746F6E00E6CF16CB07C9D023C21F8E2425D71D1F01C7058E193031328200BA93C8CB0F58CF16C90191789170E2A61801DB31E031923031E27020C88B26D658CF16CB078B1748CF16CB07C9D002C2278E2103D71D2701C7058E168200BA93C8CB0F58CF16C90191789170E2A62001DB31E05B925F04E2706DD3153191";
    Options options;
    Address address;

    public DnsRoot(Options options) {
        this.options = options;
        this.options.wc = -1;

        if (nonNull(options.address)) {
            this.address = Address.of(options.address);
        }

        if (isNull(options.code)) {
            this.options.code = CellBuilder.beginCell().fromBoc(DNS_ROOT_CODE_HEX);
        }
    }

    public DnsRoot() {
        this(Options.builder().build());
    }

    public String getName() {
        return "dnsRoot";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public Address getAddress() {
        if (isNull(address)) {
            return (createStateInit()).address;
        }
        return address;
    }

    /**
     * @return Cell cell contains nft data
     */
    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeAddress(Address.of("EQC3dNlesgVD8YbAazcauIrXBPfiVhMMr5YYk2in0Mtsz0Bz"));
        cell.storeAddress(Address.of("EQCA14o1-VWhS2efqoh_9M1b_A9DtKTuoqfmkn83AbJzwnPi"));
        cell.storeAddress(Address.of("EQB43-VCmf17O7YMd51fAvOjcMkCw46N_3JMCoegH_ZDo40e"));
        return cell.endCell();
    }

    public void deploy(Tonlib tonlib, WalletContract wallet, TweetNaclFast.Signature.KeyPair keyPair) {
        long seqno = wallet.getSeqno(tonlib);

        ExternalMessage extMsg = wallet.createTransferMessage(
                keyPair.getSecretKey(),
                this.getAddress(),
                Utils.toNano(0.1),
                seqno,
                (Cell) null, // payload body
                (byte) 3, //send mode
                this.createStateInit().stateInit
        );

        tonlib.sendRawMessage(extMsg.message.toBase64());
    }
}
