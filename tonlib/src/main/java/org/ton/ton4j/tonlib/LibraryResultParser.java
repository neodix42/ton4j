package org.ton.ton4j.tonlib;

import com.google.gson.*;
import org.ton.ton4j.tonlib.types.SmcLibraryEntry;
import org.ton.ton4j.tonlib.types.SmcLibraryResult;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

public class LibraryResultParser {

    Gson customGson;

    LibraryResultParser() {
        GsonBuilder gsonBuilder = new GsonBuilder();

        JsonDeserializer<SmcLibraryEntry> deserializerLibraryEntry = (json, typeOfT, context) -> {
            String hash = json.getAsJsonObject().get("hash").getAsString();
            String data = json.getAsJsonObject().get("data").getAsString();

            return SmcLibraryEntry.builder()
                    .hash(hash)
                    .data(data)
                    .build();
        };

        JsonDeserializer<SmcLibraryResult> deserializerLibraryResult = (json, typeOfT, context) -> {
//            TvmList list = TvmList.builder().build();
            SmcLibraryResult smcLibraryResult = SmcLibraryResult.builder().build();
//            List<Object> elements = new ArrayList<>();
//            JsonElement l = json.getAsJsonObject().get("result");
            List<SmcLibraryEntry> elements = new ArrayList<>();
            JsonArray elementsArray = json.getAsJsonObject().getAsJsonArray("result");
            if (nonNull(elementsArray)) {
                for (int j = 0; j < elementsArray.size(); j++) {
                    JsonElement element = elementsArray.get(j);
                    String elementType = element.getAsJsonObject().get("@type").getAsString();
                    elements.add(context.deserialize(element, SmcLibraryEntry.class));
                }
            }
            smcLibraryResult.setResult(elements);
            return smcLibraryResult;
        };

        gsonBuilder.registerTypeAdapter(SmcLibraryEntry.class, deserializerLibraryEntry);
        gsonBuilder.registerTypeAdapter(SmcLibraryResult.class, deserializerLibraryResult);

        customGson = gsonBuilder.setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
    }

    public SmcLibraryResult parse(String getLibResult) {
        return customGson.fromJson(getLibResult, SmcLibraryResult.class);
    }

    private Object deserializeByType(String type, JsonElement jsonElement, JsonDeserializationContext context) {
        if (type.equals("smc.libraryEntry")) {
            return context.deserialize(jsonElement, SmcLibraryEntry.class);
        } else if (type.equals("smc.libraryResult")) {
            return context.deserialize(jsonElement, SmcLibraryResult.class);
        } else return null;
    }
}
