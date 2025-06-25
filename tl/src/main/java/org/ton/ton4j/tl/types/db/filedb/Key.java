package org.ton.ton4j.tl.types.db.filedb;

import java.io.Serializable;

/**
 * Base class for all db.filedb.key.* types
 */
public abstract class Key implements Serializable {
    
    /**
     * Serialize the key to a byte array
     * @return byte array representation of the key
     */
    public abstract byte[] serialize();
}
