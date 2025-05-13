package org.ton.ton4j.smartcontract.types;


import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.address.Address;

public class InitExternalMessage {
    public Address address;
    public Cell message;
    public Cell body;
    public Cell signingMessage;
    public Cell stateInit;
    public Cell code;
    public Cell data;

    public InitExternalMessage(Address address,
                               Cell message,
                               Cell body,
                               Cell signingMessage,
                               Cell stateInit,
                               Cell code,
                               Cell data) {
        this.address = address;
        this.message = message;
        this.body = body;
        this.signingMessage = signingMessage;
        this.stateInit = stateInit;
        this.code = code;
        this.data = data;
    }
}
