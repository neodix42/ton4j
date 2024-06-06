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
public class GetMethodResult implements Serializable {
    boolean success;
    String error;
    String vm_log;
    int vm_exit_code;
    String stack; // Base64 encoded BoC serialized stack (VmStack)
    String missing_library;
    int gas_used;
}

