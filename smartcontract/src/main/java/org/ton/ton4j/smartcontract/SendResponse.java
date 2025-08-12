package org.ton.ton4j.smartcontract;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SendResponse {

    long code;
    String message;
}
