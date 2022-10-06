package org.ton.java.tonlib.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class RawAccountForm {
    final String given_type = "raw_form";
    //final String given_type = "friendly_non_bounceable";
    String raw_form;
    boolean test_only;
    Bounceable bounceable;
    NonBounceable non_bounceable;

}
