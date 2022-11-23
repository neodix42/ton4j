package org.ton.java.tonlib.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;
import java.util.List;

@Builder
@Setter
@Getter
@ToString
public class RunResult {
    List<Object> stack;
    BigInteger gas_used;
    long exit_code;
}

