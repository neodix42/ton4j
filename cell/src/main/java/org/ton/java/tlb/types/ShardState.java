package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class ShardState {
    //    _ ShardStateUnsplit = ShardState;
    //    split_state#5f327da5 left:^ShardStateUnsplit right:^ShardStateUnsplit = ShardState;
    long magic;
    ShardStateUnsplit left;
    ShardStateUnsplit right;

    private String getMagic() {
        return Long.toHexString(magic);
    }
}
