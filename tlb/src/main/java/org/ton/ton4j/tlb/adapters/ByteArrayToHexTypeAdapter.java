package org.ton.ton4j.tlb.adapters;

import com.google.gson.*;
import java.lang.reflect.Type;
import org.ton.ton4j.utils.Utils;

public class ByteArrayToHexTypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
  @Override
  public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(Utils.bytesToHex(src));
  }

  @Override
  public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    return Utils.hexToSignedBytes(json.getAsString());
  }
}
