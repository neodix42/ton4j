package org.ton.ton4j.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.ton4j.liteclient.api.config.Validators;

import java.io.Serializable;

@Builder
@Getter
@ToString
public class ResultConfig36 implements Serializable {
    private Validators validators;
}

