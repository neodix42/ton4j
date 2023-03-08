package org.ton.java.tonlib.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Setter
@Getter
@ToString
public class RawAccountForm implements Serializable {
    final String given_type = "raw_form";
    //final String given_type = "friendly_non_bounceable";
    String raw_form;
    boolean test_only;
    Bounceable bounceable;
    NonBounceable non_bounceable;

}
