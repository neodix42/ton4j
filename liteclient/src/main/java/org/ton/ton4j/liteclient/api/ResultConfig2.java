package org.ton.ton4j.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Getter
@ToString
public class ResultConfig2 implements Serializable {
    private String minterSmcAddress;
}

