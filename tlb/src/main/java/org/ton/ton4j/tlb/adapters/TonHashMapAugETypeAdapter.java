package org.ton.ton4j.tlb.adapters;

import com.google.gson.*;
import java.lang.reflect.Type;
import org.apache.commons.lang3.NotImplementedException;
import org.ton.ton4j.cell.TonHashMapAugE;

public class TonHashMapAugETypeAdapter
    implements JsonSerializer<TonHashMapAugE>, JsonDeserializer<TonHashMapAugE> {
  @Override
  public JsonElement serialize(
      TonHashMapAugE src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.toString());
  }

  @Override
  public TonHashMapAugE deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    throw new NotImplementedException();
  }
}
