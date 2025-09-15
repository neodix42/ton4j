package org.ton.ton4j.tlb.adapters;

import com.google.gson.*;
import java.lang.reflect.Type;
import org.ton.ton4j.cell.Cell;

public class CellTypeAdapter implements JsonSerializer<Cell>, JsonDeserializer<Cell> {
  @Override
  public JsonElement serialize(Cell src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.toHex(false));
  }

  @Override
  public Cell deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    return Cell.fromBoc(json.getAsString());
  }
}
