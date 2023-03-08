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
public class QueryInfo implements Serializable {
    long id;
    long valid_until;
    String body_hash; // byte[]
}
