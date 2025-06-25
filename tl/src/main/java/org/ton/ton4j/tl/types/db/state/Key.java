package org.ton.ton4j.tl.types.db.state;

import java.io.Serializable;

/**
 * Base class for all db.state.key.* types
 */
public abstract class Key implements Serializable {
    
    /**
     * Serialize the key to a byte array
     * @return byte array representation of the key
     */
    public abstract byte[] serialize();
}
