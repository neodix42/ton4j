package org.ton.java.tonlib.client;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.Encoder;

import java.io.IOException;
import java.util.UUID;

public class UUIDTonCodec implements Encoder, Decoder {
    @Override
    public Object decode(JsonIterator jsonIterator) throws IOException {
        String input = jsonIterator.readString();
        if(input == null) return null;
        return UUID.fromString(input);
    }

    @Override
    public void encode(Object o, JsonStream jsonStream) throws IOException {
        String output = "\"" + ((UUID) o).toString() + "\"";
        jsonStream.write(output.getBytes());
    }
}
