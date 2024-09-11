package org.ton.java.liteclient.api.block;

import lombok.Builder;import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Builder
@ToString
@Getter
public class Value implements Serializable {
    BigDecimal toncoins;
    private List<Currency> otherCurrencies;
}
