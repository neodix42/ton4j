package org.ton.java.adnl.liteclient.tl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * liteServer.getMasterchainInfo = liteServer.MasterchainInfo;
 * TL constructor: liteServer.getMasterchainInfo = liteServer.MasterchainInfo;
 */
public class GetMasterchainInfo extends LiteServerQuery {
    
    // TL constructor ID for liteServer.getMasterchainInfo
    private static final int CONSTRUCTOR_ID = 0x2ee6b589;
    
    @Override
    public int getConstructorId() {
        return CONSTRUCTOR_ID;
    }
    
    @Override
    protected void serializeData(ByteArrayOutputStream baos) throws IOException {
        // getMasterchainInfo has no parameters, so no additional data to serialize
    }
}
