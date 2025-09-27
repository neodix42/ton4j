package org.ton.ton4j.tlb.adapters;

import com.google.gson.*;
import java.lang.reflect.Type;
import org.apache.commons.lang3.NotImplementedException;
import org.ton.ton4j.cell.TonHashMapE;

public class TonHashMapETypeAdapter
    implements JsonSerializer<TonHashMapE>, JsonDeserializer<TonHashMapE> {
  @Override
  public JsonElement serialize(TonHashMapE src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.toString());
  }

  @Override
  public TonHashMapE deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    throw new NotImplementedException();
  }
}
