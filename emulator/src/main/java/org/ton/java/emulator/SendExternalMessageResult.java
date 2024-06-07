package org.ton.java.emulator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Setter
@Getter
@ToString
public class SendExternalMessageResult implements Serializable {
    boolean success;
    String new_code; // Base64 boc decoded new code cell
    String new_data; // Base64 boc decoded new data cell
    String error;
    String vm_log;
    int vm_exit_code;
    String stack; // Base64 encoded BoC serialized stack (VmStack)
    String missing_library;
    int gas_used;
    String actions; // Base64 boc decoded actions cell of type (OutList n)
}

