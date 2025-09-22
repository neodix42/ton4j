package org.ton.ton4j.tlb.adapters;

import com.google.gson.*;
import java.lang.reflect.Type;
import org.ton.ton4j.bitstring.BitString;

public class BitStringTypeAdapter
    implements JsonSerializer<BitString>, JsonDeserializer<BitString> {
  @Override
  public JsonElement serialize(BitString src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.toHex());
  }

  @Override
  public BitString deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    return new BitString(json.getAsString().getBytes()); // todo
  }
}
