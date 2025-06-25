package org.ton.ton4j.tl.types.db.lt;

import java.io.Serializable;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * Base class for db.lt.Key types
 * </pre>
 */
public abstract class Key implements Serializable {
  public abstract byte[] serialize();
}
