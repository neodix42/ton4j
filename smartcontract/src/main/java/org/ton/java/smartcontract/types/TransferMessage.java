package org.ton.java.smartcontract.types;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;

public class TransferMessage extends ExternalMessage {

    public TransferMessage(Address address, Cell message, Cell body, byte[] signature, Cell signingMessage, Cell stateInit, Cell code, Cell data) {
        super(address, message, body, signature, signingMessage, stateInit, code, data);
    }
}
