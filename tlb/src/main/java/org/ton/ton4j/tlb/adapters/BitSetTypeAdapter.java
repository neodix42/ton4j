package org.ton.ton4j.tlb.adapters;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.BitSet;
import org.ton.ton4j.utils.Utils;

public class BitSetTypeAdapter implements JsonSerializer<BitSet>, JsonDeserializer<BitSet> {
  @Override
  public JsonElement serialize(BitSet src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.toString());
  }

  @Override
  public BitSet deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    return BitSet.valueOf(Utils.bitStringToByteArray(json.getAsString()));
  }
}
