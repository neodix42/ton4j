package org.ton.ton4j.tlb.adapters;

import com.google.gson.*;
import java.lang.reflect.Type;
import org.apache.commons.lang3.NotImplementedException;
import org.ton.ton4j.cell.TonHashMap;

public class TonHashMapTypeAdapter
    implements JsonSerializer<TonHashMap>, JsonDeserializer<TonHashMap> {
  @Override
  public JsonElement serialize(TonHashMap src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.toString());
  }

  @Override
  public TonHashMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    throw new NotImplementedException();
  }
}
