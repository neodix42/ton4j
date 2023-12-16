package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RawAccountForm {
    private String given_type;
    private String raw_form;
    private boolean test_only;
    private Bounceable bounceable;
    private NonBounceable non_bounceable;
}
