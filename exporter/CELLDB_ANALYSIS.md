# CellDB Storage Analysis

## Overview

The CellDB uses a **dual storage approach** with two distinct types of entries in the RocksDB database.

## Storage Structure

### 1. Metadata Entries

**Key Format:**
- Regular entries: `"desc" + SHA256(TL-serialized block_id)`
- Empty/sentinel entry: `"desczero"`

**Value Format:** TL-serialized `db.celldb.value` containing:
```
db.celldb.value block_id:tonNode.blockIdExt prev:int256 next:int256 root_hash:int256 = db.celldb.Value;
```

**Fields:**
- `block_id`: The block identifier (BlockIdExt)
- `prev`: Previous entry hash in linked list (32 bytes)
- `next`: Next entry hash in linked list (32 bytes)
- `root_hash`: **Hash of the root cell for this block state** (32 bytes) - **This is the key to cell data!**

### 2. Cell Data Entries

**Key Format:**
- Raw cell hash (32 bytes, no "desc" prefix)

**Value Format:**
- Serialized cell content (the actual cell data)

## Key Insight: The Connection

The `root_hash` field in metadata entries is **the bridge** to actual cell data:

```
Metadata Entry: "desc" + hash -> {block_id, prev, next, root_hash}
                                                        |
                                                        v
Cell Data Entry: root_hash (raw bytes) -> serialized cell content
```
