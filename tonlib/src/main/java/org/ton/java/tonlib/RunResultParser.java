package org.ton.java.tonlib;

import com.google.gson.*;
import org.ton.java.tonlib.types.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

public class RunResultParser {

    // Static and final Gson instance to avoid recreation for each parser instance
    private static final Gson customGson;
    
    // Static initialization of deserializers and Gson
    static {
        GsonBuilder gsonBuilder = new GsonBuilder();

        // Optimized deserializers with direct access to JSON elements
        JsonDeserializer<TvmStackEntryNumber> deserializerStackEntryNumber = (json, typeOfT, context) -> {
            JsonElement i2 = json.getAsJsonObject().get("number").getAsJsonObject();
            String i3 = i2.getAsJsonObject().get("number").getAsString();
            TvmNumber n = TvmNumber.builder().number(i3).build();
            return TvmStackEntryNumber.builder().number(n).build();
        };

        JsonDeserializer<TvmStackEntryCell> deserializerStackEntryCell = (json, typeOfT, context) -> {
            JsonElement i2 = json.getAsJsonObject().get("cell").getAsJsonObject();
            String i3 = i2.getAsJsonObject().get("bytes").getAsString();
            TvmCell n = TvmCell.builder().bytes(i3).build();
            return TvmStackEntryCell.builder().cell(n).build();
        };

        JsonDeserializer<TvmStackEntrySlice> deserializerStackEntrySlice = (json, typeOfT, context) -> {
            JsonElement i2 = json.getAsJsonObject().get("slice").getAsJsonObject();
            String i3 = i2.getAsJsonObject().get("bytes").getAsString();
            TvmSlice n = TvmSlice.builder().bytes(i3).build();
            return TvmStackEntrySlice.builder().slice(n).build();
        };

        JsonDeserializer<TvmStackEntryList> deserializerStackEntryList = (json, typeOfT, context) -> {
            JsonElement l = json.getAsJsonObject().get("list");
            JsonArray elementsArray = l.getAsJsonObject().getAsJsonArray("elements");
            
            // Pre-allocate the list with the exact size needed
            List<Object> elements = nonNull(elementsArray) ? new ArrayList<>(elementsArray.size()) : new ArrayList<>(0);
            
            if (nonNull(elementsArray)) {
                for (int j = 0; j < elementsArray.size(); j++) {
                    JsonElement element = elementsArray.get(j);
                    String elementType = element.getAsJsonObject().get("@type").getAsString();
                    elements.add(deserializeByType(elementType, element, context));
                }
            }
            
            TvmList list = TvmList.builder().elements(elements).build();
            return TvmStackEntryList.builder().list(list).build();
        };

        JsonDeserializer<TvmStackEntryTuple> deserializerStackEntryTuple = (json, typeOfT, context) -> {
            JsonElement l = json.getAsJsonObject().get("tuple");
            JsonArray elementsArray = l.getAsJsonObject().getAsJsonArray("elements");
            
            // Pre-allocate the list with the exact size needed
            List<Object> elements = nonNull(elementsArray) ? new ArrayList<>(elementsArray.size()) : new ArrayList<>(0);
            
            if (nonNull(elementsArray)) {
                for (int j = 0; j < elementsArray.size(); j++) {
                    JsonElement element = elementsArray.get(j);
                    String elementType = element.getAsJsonObject().get("@type").getAsString();
                    elements.add(deserializeByType(elementType, element, context));
                }
            }
            
            TvmTuple tuple = TvmTuple.builder().elements(elements).build();
            return TvmStackEntryTuple.builder().tuple(tuple).build();
        };

        JsonDeserializer<RunResult> deserializer = (json, typeOfT, context) -> {
            JsonObject jsonObject = json.getAsJsonObject();
            String el = jsonObject.get("@type").toString();
            if (!el.contains("smc.runResult")) {
                throw new Error("malformed response from run_method");
            }
            
            JsonArray stackArray = jsonObject.get("stack").getAsJsonArray();
            // Pre-allocate the list with the exact size needed
            List<Object> stack = new ArrayList<>(stackArray.size());

            BigInteger gasUsed = jsonObject.get("gas_used").getAsBigInteger();
            long exitCode = jsonObject.get("exit_code").getAsLong();

            for (int i = 0; i < stackArray.size(); i++) {
                JsonElement element = stackArray.get(i);
                String elementType = element.getAsJsonObject().get("@type").getAsString();
                stack.add(deserializeByType(elementType, element, context));
            }
            
            return RunResult.builder()
                    .stack(stack)
                    .exit_code(exitCode)
                    .gas_used(gasUsed)
                    .build();
        };

        gsonBuilder.registerTypeAdapter(RunResult.class, deserializer);
        gsonBuilder.registerTypeAdapter(TvmStackEntryNumber.class, deserializerStackEntryNumber);
        gsonBuilder.registerTypeAdapter(TvmStackEntryCell.class, deserializerStackEntryCell);
        gsonBuilder.registerTypeAdapter(TvmStackEntrySlice.class, deserializerStackEntrySlice);
        gsonBuilder.registerTypeAdapter(TvmStackEntryList.class, deserializerStackEntryList);
        gsonBuilder.registerTypeAdapter(TvmStackEntryTuple.class, deserializerStackEntryTuple);

        customGson = gsonBuilder
                .setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL)
                .disableHtmlEscaping()
                .create();
    }
    
    // No-args constructor - nothing to initialize as Gson is static
    RunResultParser() {
        // Empty constructor as all initialization is done in static block
    }

    public RunResult parse(String runMethodResult) {
        RunResult result;
        try {
            result = customGson.fromJson(runMethodResult, RunResult.class);
        } catch (Throwable e) {
            result = RunResult.builder()
                    .exit_code(-1)
                    .build();
        }
        return result;
    }

    private static Object deserializeByType(String type, JsonElement jsonElement, JsonDeserializationContext context) {
        Class<?> clazz;
        switch (type) {
            case "tvm.stackEntryNumber":
                clazz = TvmStackEntryNumber.class;
                break;
            case "tvm.stackEntryCell":
                clazz = TvmStackEntryCell.class;
                break;
            case "tvm.stackEntrySlice":
                clazz = TvmStackEntrySlice.class;
                break;
            case "tvm.stackEntryList":
                clazz = TvmStackEntryList.class;
                break;
            case "tvm.stackEntryTuple":
                clazz = TvmStackEntryTuple.class;
                break;
            default:
                clazz = null;
        }
        return clazz != null ? context.deserialize(jsonElement, clazz) : null;
    }
}
