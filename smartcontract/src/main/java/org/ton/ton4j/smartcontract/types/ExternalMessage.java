package org.ton.ton4j.smartcontract.types;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.address.Address;

public class ExternalMessage {
    public Address address;
    public Cell message;
    public Cell body;
    public byte[] signature;
    public Cell signingMessage;
    public Cell stateInit;
    public Cell code;
    public Cell data;

    public ExternalMessage(Address address, Cell message, Cell body, byte[] signature, Cell signingMessage, Cell stateInit, Cell code, Cell data) {
        this.address = address;
        this.message = message;
        this.body = body;
        this.signature = signature;
        this.signingMessage = signingMessage;
        this.stateInit = stateInit;
        this.code = code;
        this.data = data;
    }
}
