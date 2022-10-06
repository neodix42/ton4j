package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class NewKeyQuery {
    @SerializedName("@type")
    final String type = "createNewKey";
    String mnemonic_password;
    String random_extra_seed;
    String local_password;
}
