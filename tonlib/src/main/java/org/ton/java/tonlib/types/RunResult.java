package org.ton.java.tonlib.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

@Builder
@Setter
@Getter
@ToString
public class RunResult implements Serializable {
    List<Object> stack;
    BigInteger gas_used;
    long exit_code;
}

