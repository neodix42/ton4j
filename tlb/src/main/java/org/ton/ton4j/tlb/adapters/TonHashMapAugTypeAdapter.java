package org.ton.ton4j.tlb.adapters;

import com.google.gson.*;
import java.lang.reflect.Type;
import org.apache.commons.lang3.NotImplementedException;
import org.ton.ton4j.cell.TonHashMapAug;

public class TonHashMapAugTypeAdapter
    implements JsonSerializer<TonHashMapAug>, JsonDeserializer<TonHashMapAug> {
  @Override
  public JsonElement serialize(
      TonHashMapAug src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.toString());
  }

  @Override
  public TonHashMapAug deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    throw new NotImplementedException();
  }
}
