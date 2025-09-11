package org.ton.ton4j.tlb.adapters;

import com.google.gson.*;
import java.lang.reflect.Type;
import org.ton.ton4j.cell.TonHashMapAugE;
import org.ton.ton4j.utils.Utils;

public class TonHashMapAugETypeAdapter
    implements JsonSerializer<TonHashMapAugE>, JsonDeserializer<byte[]> {
  @Override
  public JsonElement serialize(
      TonHashMapAugE src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.toString());
  }

  @Override
  public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    return Utils.hexToSignedBytes(json.getAsString());
  }
}
