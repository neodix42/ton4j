package org.ton.java.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.java.liteclient.api.config.Validators;

import java.io.Serializable;

@Builder
@Getter
@ToString
public class ResultConfig36 implements Serializable {
    private Validators validators;
}

