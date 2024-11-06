package org.ton.java;

import lombok.Builder;
import lombok.Data;
import org.ton.java.emulator.tvm.GetMethodResult;
import org.ton.java.tonlib.types.RunResult;

@Builder
@Data
public class GetterResult {
  GetMethodResult emulatorResult;
  RunResult tonlibResult;
}
