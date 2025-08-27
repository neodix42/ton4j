package org.ton.ton4j.smartcontract;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Builder
@Data
public class SendResponse {

    long code;
    String message;
}
