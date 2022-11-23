package org.ton.java.tonlib;

import com.google.gson.*;
import org.ton.java.tonlib.types.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

public class RunResultParser {

    Gson customGson;

    RunResultParser() {
        GsonBuilder gsonBuilder = new GsonBuilder();

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
            TvmList list = TvmList.builder().build();
            TvmStackEntryList entryList = TvmStackEntryList.builder().build();
            List<Object> elements = new ArrayList<>();
            JsonElement l = json.getAsJsonObject().get("list");
            JsonArray elementsArray = l.getAsJsonObject().getAsJsonArray("elements");
            if (nonNull(elementsArray)) {
                for (int j = 0; j < elementsArray.size(); j++) {
                    JsonElement element = elementsArray.get(j);
                    String elementType = element.getAsJsonObject().get("@type").getAsString();
                    elements.add(deserializeByType(elementType, element, context));
//                    switch (elType2) {
//                        case "tvm.stackEntryNumber" -> {
//                            TvmStackEntryNumber n2 = context.deserialize(elInner2, TvmStackEntryNumber.class);
//                            elements.add(n2);
//                        }
//                        case "tvm.stackEntryCell" -> {
//                            TvmStackEntryCell c2 = context.deserialize(elInner2, TvmStackEntryCell.class);
//                            elements.add(c2);
//                        }
//                        case "tvm.stackEntrySlice" -> {
//                            TvmStackEntrySlice s2 = context.deserialize(elInner2, TvmStackEntrySlice.class);
//                            elements.add(s2);
//                        }
//                        case "tvm.stackEntryList" -> {
//                            TvmStackEntryList l2 = context.deserialize(elInner2, TvmStackEntryList.class);
//                            elements.add(l2);
//                        }
//                        case "tvm.stackEntryTuple" -> {
//                            TvmStackEntryTuple t2 = context.deserialize(elInner2, TvmStackEntryTuple.class);
//                            elements.add(t2);
//                        }
//                    }
                }
            }
            list.setElements(elements);
            entryList.setList(list);
            return entryList;
        };

        JsonDeserializer<TvmStackEntryTuple> deserializerStackEntryTuple = (json, typeOfT, context) -> {
            TvmTuple tuple = TvmTuple.builder().build();
            TvmStackEntryTuple entryList = TvmStackEntryTuple.builder().build();
            List<Object> elements = new ArrayList<>();
            JsonElement l = json.getAsJsonObject().get("tuple");
            JsonArray elementsArray = l.getAsJsonObject().getAsJsonArray("elements");
            if (nonNull(elementsArray)) {
                for (int j = 0; j < elementsArray.size(); j++) {
                    JsonElement element = elementsArray.get(j);
                    String elementType = element.getAsJsonObject().get("@type").getAsString();
                    elements.add(deserializeByType(elementType, element, context));
//                    switch (elType2) {
//                        case "tvm.stackEntryNumber":
//                            TvmStackEntryNumber n2 = context.deserialize(elInner2, TvmStackEntryNumber.class);
//                            elements.add(n2);
//                            break;
//                        case "tvm.stackEntryCell":
//                            TvmStackEntryCell c2 = context.deserialize(elInner2, TvmStackEntryCell.class);
//                            elements.add(c2);
//                            break;
//                        case "tvm.stackEntrySlice":
//                            TvmStackEntrySlice s2 = context.deserialize(elInner2, TvmStackEntrySlice.class);
//                            elements.add(s2);
//                            break;
//                        case "tvm.stackEntryList":
//                            TvmStackEntryList l2 = context.deserialize(elInner2, TvmStackEntryList.class);
//                            elements.add(l2);
//                            break;
//                        case "tvm.stackEntryTuple":
//                            TvmStackEntryTuple t2 = context.deserialize(elInner2, TvmStackEntryTuple.class);
//                            elements.add(t2);
//                            break;
//                    }
                }
            }
            tuple.setElements(elements);
            entryList.setTuple(tuple);
            return entryList;
        };


        JsonDeserializer<RunResult> deserializer = (json, typeOfT, context) -> {

            List<Object> stack = new ArrayList<>();

            JsonObject jsonObject = json.getAsJsonObject();
            String el = jsonObject.get("@type").toString();
            if (!el.contains("smc.runResult")) {
                throw new Error("json string does not come from runmethod result");
            }
            JsonElement stackList = jsonObject.get("stack");

            BigInteger gasUsed = jsonObject.get("gas_used").getAsBigInteger();
            long exitCode = jsonObject.get("exit_code").getAsLong();

            for (int i = 0; i < stackList.getAsJsonArray().size(); i++) {
                JsonElement element = stackList.getAsJsonArray().get(i);
                String elementType = element.getAsJsonObject().get("@type").getAsString();
                stack.add(deserializeByType(elementType, element, context));
//                switch (elType) {
//                    case "tvm.stackEntryNumber":
//                        TvmStackEntryNumber n1 = context.deserialize(elInner, TvmStackEntryNumber.class);
//                        stack.add(n1);
//                        break;
//                    case "tvm.stackEntryCell":
//                        TvmStackEntryCell c1 = context.deserialize(elInner, TvmStackEntryCell.class);
//                        stack.add(c1);
//                        break;
//                    case "tvm.stackEntrySlice":
//                        TvmStackEntrySlice s1 = context.deserialize(elInner, TvmStackEntrySlice.class);
//                        stack.add(s1);
//                        break;
//                    case "tvm.stackEntryList":
//                        TvmStackEntryList l1 = context.deserialize(elInner, TvmStackEntryList.class);
//                        stack.add(l1);
//                        break;
//                    case "tvm.stackEntryTuple":
//                        TvmStackEntryTuple t1 = context.deserialize(elInner, TvmStackEntryTuple.class);
//                        stack.add(t1);
//                        break;
//                }
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

        customGson = gsonBuilder.setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
    }

    public RunResult parse(String runMethodResult) {
        return customGson.fromJson(runMethodResult, RunResult.class);
    }

    private Object deserializeByType(String type, JsonElement jsonElement, JsonDeserializationContext context) {
        return switch (type) {
            case "tvm.stackEntryNumber" -> context.deserialize(jsonElement, TvmStackEntryNumber.class);
            case "tvm.stackEntryCell" -> context.deserialize(jsonElement, TvmStackEntryCell.class);
            case "tvm.stackEntrySlice" -> context.deserialize(jsonElement, TvmStackEntrySlice.class);
            case "tvm.stackEntryList" -> context.deserialize(jsonElement, TvmStackEntryList.class);
            case "tvm.stackEntryTuple" -> context.deserialize(jsonElement, TvmStackEntryTuple.class);
            default -> null;
        };
    }
}
