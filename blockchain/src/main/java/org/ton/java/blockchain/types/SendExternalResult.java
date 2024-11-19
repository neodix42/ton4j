package org.ton.java.blockchain.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.emulator.EmulateTransactionResult;
import org.ton.java.tonlib.types.ExtMessageInfo;

@Builder
@Data
public class SendExternalResult {
    EmulateTransactionResult emulatorResult;
    ExtMessageInfo tonlibResult;
}
