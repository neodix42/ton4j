package org.ton.ton4j.exporter.types;

/** Cell type enumeration based on TON cell special types. */
public enum CellType {
  ORDINARY(0, "Ordinary cell"),
  PRUNED_BRANCH(1, "Pruned branch cell"),
  LIBRARY_REFERENCE(2, "Library reference cell"),
  MERKLE_PROOF(3, "Merkle proof cell"),
  MERKLE_UPDATE(4, "Merkle update cell");

  private final int typeId;
  private final String description;

  CellType(int typeId, String description) {
    this.typeId = typeId;
    this.description = description;
  }

  public int getTypeId() {
    return typeId;
  }

  public String getDescription() {
    return description;
  }

  public static CellType fromTypeId(int typeId) {
    for (CellType type : values()) {
      if (type.typeId == typeId) {
        return type;
      }
    }
    return ORDINARY;
  }
}
