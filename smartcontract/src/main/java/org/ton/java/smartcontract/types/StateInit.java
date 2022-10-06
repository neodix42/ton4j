package org.ton.java.smartcontract.types;


import org.ton.java.cell.Cell;
import org.ton.java.address.Address;

public class StateInit {
    public Cell stateInit;
    public Address address;
    public Cell code;
    public Cell data;

    public StateInit(Cell stateInit, Address address, Cell code, Cell data) {
        this.stateInit = stateInit;
        this.address = address;
        this.code = code;
        this.data = data;
    }
}
