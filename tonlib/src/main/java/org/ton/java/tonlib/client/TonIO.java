package org.ton.java.tonlib.client;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.output.EncodingMode;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.DecodingMode;
import com.jsoniter.spi.JsoniterSpi;
import com.sun.jna.Native;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.base.TypeToClassMap;
import org.ton.java.tonlib.base.TypedAsyncObject;
import org.ton.java.tonlib.jna.TonlibJsonI;
import org.ton.java.tonlib.queries.VerbosityLevelQuery;
import org.ton.java.tonlib.types.VerbosityLevel;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.util.Objects.isNull;

public class TonIO {
    @Getter
    private final ConcurrentLinkedQueue<String> in = new ConcurrentLinkedQueue<>();
    @Getter
    private final ConcurrentHashMap<UUID, Object> out = new ConcurrentHashMap<>();
    private final TonlibJsonI tonlibJson;
    private final long tonlib;
    private final Executor executor = Executors.newFixedThreadPool(2);
    @Getter
    private final TonClient tonClient;

    @SneakyThrows
    public TonIO(String pathToTonlibSharedLib,String configData) {
        JsonIterator.setMode(DecodingMode.REFLECTION_MODE);
        JsonStream.setMode(EncodingMode.REFLECTION_MODE);
        JsoniterSpi.registerTypeDecoder(UUID.class,new UUIDTonCodec());
        JsoniterSpi.registerTypeDecoder(UUID.class,new UUIDTonCodec());
        this.tonlibJson = Native.load(pathToTonlibSharedLib, TonlibJsonI.class);
        this.tonlib = tonlibJson.tonlib_client_json_create();
        VerbosityLevelQuery verbosityLevelQuery = VerbosityLevelQuery.builder().new_verbosity_level(VerbosityLevel.INFO.ordinal()).build();
        verbosityLevelQuery.setType(verbosityLevelQuery.getTypeObjectName());
        tonlibJson.tonlib_client_json_send(tonlib, JsonStream.serialize(verbosityLevelQuery));
        String result = tonlibJson.tonlib_client_json_receive(tonlib, 5000.0);
        System.out.println("set verbosityLevel result: " + result);
        String initTemplate = new String(Tonlib.class.getClassLoader().getResourceAsStream("init.json").readAllBytes());
        String config = initTemplate.replace("\"CFG_PLACEHOLDER\"",JsonStream.serialize(configData)).replace("\"KEYSTORE_TYPE\""," \"keystore_type\": { \"@type\": \"keyStoreTypeInMemory\" }");
        System.out.println(config);
        tonlibJson.tonlib_client_json_send(tonlib, config);
        result = tonlibJson.tonlib_client_json_receive(tonlib, 5000.0);
        System.out.println("set tonlib configuration result " + result);
        executor.execute(new WriterThread());
        executor.execute(new ReaderThread());
        System.out.println("Started TonLib I/O!");
        tonClient = new TonClient(this);
    }

    public void submitRequest(String request){
        in.add(request);
        synchronized (in){
            in.notify();
        }
    }

    private class WriterThread implements Runnable {
        @SneakyThrows
        @Override
        public void run() {

            while (!Thread.interrupted()) {
                while (in.isEmpty()) {
                    synchronized (in){
                        in.wait(100);
                    }
                }
                while(!in.isEmpty()){
                    String request = in.poll();
                    tonlibJson.tonlib_client_json_send(tonlib,request);
                }
            }
        }
    }


    private class ReaderThread implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                String result = tonlibJson.tonlib_client_json_receive(tonlib, 1.0);
                if (isNull(result)) {
                    continue;
                }
                Any t = JsonIterator.deserialize(result);
                var x = t.as(TypeToClassMap.classes.get(t.toString("@type")));
                if(t.get("@extra").as(UUID.class) != null) {
                    out.putIfAbsent(t.get("@extra").as(UUID.class), x);
                    synchronized (out) {
                        out.notifyAll();
                    }
                }
            }
        }
    }


}
