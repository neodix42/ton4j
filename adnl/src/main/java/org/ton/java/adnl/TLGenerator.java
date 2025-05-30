package org.ton.java.adnl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * TLGenerator for ADNL protocol message serialization/deserialization
 * Java implementation for TON ADNL protocol
 */
public class TLGenerator {
    private static final Logger logger = Logger.getLogger(TLGenerator.class.getName());
    private final String path;
    private final TLRegistrator registrator;
    
    /**
     * Create a TLGenerator with the specified path
     * @param path Path to TL schema files
     */
    public TLGenerator(String path) {
        this(path, new TLRegistrator());
    }
    
    /**
     * Create a TLGenerator with the specified path and registrator
     * @param path Path to TL schema files
     * @param registrator TL registrator
     */
    public TLGenerator(String path, TLRegistrator registrator) {
        this.path = path;
        this.registrator = registrator;
    }
    
    /**
     * Create a TLGenerator with default ADNL schemas
     * @return TLSchemas instance
     */
    public static TLSchemas withDefaultSchemas() {
        List<TLSchema> schemas = new ArrayList<>();
        
        // Register ADNL message schemas (TCP-compatible only)
        schemas.add(new TLSchema(intToBytes(0x7af98bb4), "adnl.message.query", "adnl.Message", 
                mapOf("query_id", "bytes", "query", "bytes")));
        schemas.add(new TLSchema(intToBytes(0x4c2d4977), "adnl.message.answer", "adnl.Message", 
                mapOf("query_id", "bytes", "answer", "bytes")));
        schemas.add(new TLSchema(intToBytes(0x7e5e5fce), "adnl.message.custom", "adnl.Message", 
                mapOf("data", "bytes")));
        schemas.add(new TLSchema(intToBytes(0x4c1c2a16), "adnl.message.part", "adnl.Message", 
                mapOf("hash", "bytes", "total_size", "int", "offset", "int", "data", "bytes")));
        
        // Register ADNL packet schemas
        schemas.add(new TLSchema(intToBytes(0x7c42be31), "adnl.packetContents", "adnl.PacketContents", 
                mapOf("flags", "int", "from", "bytes", "from_short", "bytes", "message", "bytes", 
                      "messages", "bytes", "address", "bytes", "priority_address", "bytes", 
                      "seqno", "int", "confirm_seqno", "int", "recv_addr_list_version", "int", 
                      "recv_priority_addr_list_version", "int", "reinit_date", "int", 
                      "dst_reinit_date", "int", "signature", "bytes")));
        
        // Register DHT schemas
        schemas.add(new TLSchema(intToBytes(0x7b9ef7b3), "dht.ping", "dht.Ping", 
                mapOf("random_id", "long")));
        schemas.add(new TLSchema(intToBytes(0x4fb53ffb), "dht.getSignedAddressList", "dht.GetSignedAddressList", 
                new HashMap<>()));
        
        // Register pub.ed25519 schema
        schemas.add(new TLSchema(intToBytes(0x8e81d396), "pub.ed25519", "pub.Ed25519", 
                mapOf("key", "bytes")));
        
        return new TLSchemas(schemas);
    }
    
    /**
     * Generate TL schemas from files
     * @return TLSchemas instance
     */
    public TLSchemas generate() {
        List<TLSchema> result = new ArrayList<>();
        File path = new File(this.path);
        
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    result.addAll(fromFile(file.getAbsolutePath()));
                }
            }
        } else {
            result.addAll(fromFile(this.path));
        }
        
        return new TLSchemas(result);
    }
    
    /**
     * Generate TL schemas from a file
     * @param filePath Path to TL schema file
     * @return List of TLSchema instances
     */
    public List<TLSchema> fromFile(String filePath) {
        List<TLSchema> result = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder temp = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                String stripped = line.trim();
                
                if (stripped.isEmpty() || stripped.startsWith("//") || stripped.startsWith("---")) {
                    continue;
                }
                
                if (!stripped.contains(";")) {
                    temp.append(stripped).append(" ");
                    continue;
                } else {
                    stripped = temp.toString() + stripped;
                    temp = new StringBuilder();
                }
                
                result.add(registrator.register(stripped));
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading TL schema file", e);
        }
        
        return result;
    }
    
    /**
     * Convert int to bytes
     * @param value Integer value
     * @return Byte array
     */
    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array();
    }
    
    /**
     * Convert bytes to hex string
     * @param bytes Byte array
     * @return Hex string
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
    
    /**
     * Convert hex string to bytes
     * @param hex Hex string
     * @return Byte array
     */
    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    
    /**
     * Create a map from key-value pairs
     * @param keyValues Key-value pairs
     * @return Map
     */
    private static <K, V> Map<K, V> mapOf(Object... keyValues) {
        Map<K, V> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            @SuppressWarnings("unchecked")
            K key = (K) keyValues[i];
            @SuppressWarnings("unchecked")
            V value = (V) keyValues[i + 1];
            map.put(key, value);
        }
        return map;
    }
    
    /**
     * TL Error class
     */
    public static class TLError extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        public TLError(String message) {
            super(message);
        }
        
        public TLError(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * TL Schema class
     */
    public static class TLSchema {
        private final byte[] id;
        private final String name;
        private final String className;
        private final Map<String, String> args;
        private boolean boxed = false;
        
        public TLSchema(byte[] id, String name, String className, Map<String, String> args) {
            this.id = id;
            this.name = name;
            this.className = className;
            this.args = args;
        }
        
        public byte[] getId() {
            return id;
        }
        
        public byte[] getLittleId() {
            byte[] reversed = new byte[id.length];
            for (int i = 0; i < id.length; i++) {
                reversed[i] = id[id.length - 1 - i];
            }
            return reversed;
        }
        
        public String getName() {
            return name;
        }
        
        public String getClassName() {
            return className;
        }
        
        public Map<String, String> getArgs() {
            return args;
        }
        
        public boolean isBoxed() {
            return boxed;
        }
        
        public void setBoxed(boolean boxed) {
            this.boxed = boxed;
        }
        
        public boolean isEmpty() {
            return id == null || name == null;
        }
        
        public static TLSchema empty() {
            return new TLSchema(null, null, null, new HashMap<>());
        }
        
        @Override
        public String toString() {
            return "TLSchema " + name + " №" + TLGenerator.bytesToHex(id) + " with args " + args;
        }
    }
    
    /**
     * TL Schemas class
     */
    public static class TLSchemas {
        private static final Map<String, Integer> BASE_TYPES = new HashMap<>();
        static {
            BASE_TYPES.put("Bool", 4);
            BASE_TYPES.put("#", 4);
            BASE_TYPES.put("int", 4);
            BASE_TYPES.put("long", 8);
            BASE_TYPES.put("int128", 16);
            BASE_TYPES.put("int256", 32);
            BASE_TYPES.put("string", null);
            BASE_TYPES.put("bytes", null);
            BASE_TYPES.put("vector", null);
        }
        
        private final List<TLSchema> schemas;
        private final Map<ByteArrayWrapper, TLSchema> idMap = new HashMap<>();
        private final Map<String, TLSchema> nameMap = new HashMap<>();
        private final Map<String, List<TLSchema>> classNameMap = new HashMap<>();
        private final boolean autoDeserialize;
        private final Map<String, Set<String>> untouchables = new HashMap<>();
        
        public TLSchemas(List<TLSchema> schemas) {
            this(schemas, true);
        }
        
        public TLSchemas(List<TLSchema> schemas, boolean autoDeserialize) {
            this.schemas = schemas;
            this.autoDeserialize = autoDeserialize;
            generateMap();
            setDefaultUntouchables();
        }
        
        private void generateMap() {
            for (TLSchema schema : schemas) {
                if (schema.isEmpty()) {
                    continue;
                }
                idMap.put(new ByteArrayWrapper(schema.getId()), schema);
                nameMap.put(schema.getName(), schema);
                
                List<TLSchema> classSchemas = classNameMap.getOrDefault(schema.getClassName(), new ArrayList<>());
                classSchemas.add(schema);
                classNameMap.put(schema.getClassName(), classSchemas);
            }
        }
        
        private void setDefaultUntouchables() {
            Set<String> adnlMessagePartFields = new HashSet<>();
            adnlMessagePartFields.add("data");
            untouchables.put("adnl.message.part", adnlMessagePartFields);
            
            Set<String> overlayBroadcastFecFields = new HashSet<>();
            overlayBroadcastFecFields.add("data");
            untouchables.put("overlay.broadcastFec", overlayBroadcastFecFields);
        }
        
        public TLSchema getById(byte[] id, ByteOrder byteOrder) {
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                byte[] reversed = new byte[id.length];
                for (int i = 0; i < id.length; i++) {
                    reversed[i] = id[id.length - 1 - i];
                }
                id = reversed;
            }
            return idMap.get(new ByteArrayWrapper(id));
        }
        
        public TLSchema getById(int id, ByteOrder byteOrder) {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(byteOrder).putInt(id);
            buffer.flip();
            byte[] bytes = new byte[4];
            buffer.get(bytes);
            return getById(bytes, ByteOrder.BIG_ENDIAN);
        }
        
        public TLSchema getByName(String name) {
            return nameMap.get(name);
        }
        
        public List<TLSchema> getByClassName(String className) {
            return classNameMap.get(className);
        }
        
        public byte[] serialize(TLSchema schema, Map<String, Object> data, boolean boxed) {
            logger.log(Level.FINEST, "Serializing schema " + schema);
            
            ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
            
            if (boxed) {
                buffer.put(schema.getLittleId());
            }
            
            for (Map.Entry<String, String> entry : schema.getArgs().entrySet()) {
                String field = entry.getKey();
                String type = entry.getValue();
                
                if (type.contains("?")) {
                    int index = Integer.parseInt(type.substring(type.indexOf('.') + 1, type.indexOf('?')));
                    int flags = (int) data.getOrDefault("mode", data.getOrDefault("flags", 0));
                    if ((flags & (1 << index)) == 0) {
                        continue;
                    }
                    type = type.split("\\?")[1];
                }
                
                Object value = data.get(field);
                if (value == null) {
                    continue;
                }
                
                buffer.put(serializeField(type, value));
            }
            
            byte[] result = new byte[buffer.position()];
            buffer.flip();
            buffer.get(result);
            
            logger.log(Level.FINEST, "Serialization result for schema " + schema + " is " + TLGenerator.bytesToHex(result));
            return result;
        }
        
        private byte[] serializeField(String type, Object value) {
            logger.log(Level.FINEST, "Serializing " + type + " with value " + value);
            
            ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
            
            if (BASE_TYPES.containsKey(type)) {
                Integer byteLen = BASE_TYPES.get(type);
                
                if (byteLen != null) {
                    if (value instanceof Boolean) {
                        buffer.put(((Boolean) value) ? new byte[]{(byte) 0x99, (byte) 0x72, (byte) 0x75, (byte) 0xb5} : 
                                                       new byte[]{(byte) 0xbc, (byte) 0x79, (byte) 0x97, (byte) 0x37});
                    } else if (value instanceof byte[]) {
                        byte[] bytes = (byte[]) value;
                        byte[] reversed = new byte[byteLen];
                        for (int i = 0; i < Math.min(bytes.length, byteLen); i++) {
                            reversed[i] = bytes[bytes.length - 1 - i];
                        }
                        buffer.put(reversed);
                        buffer.put(new byte[Math.max(0, byteLen - bytes.length)]);
                    } else if (value instanceof Integer) {
                        buffer.order(ByteOrder.LITTLE_ENDIAN).putInt((Integer) value);
                    } else if (value instanceof Long) {
                        buffer.order(ByteOrder.LITTLE_ENDIAN).putLong((Long) value);
                    } else if (value instanceof String) {
                        buffer.put(TLGenerator.hexToBytes((String) value));
                    }
                } else {
                    if (type.equals("bytes")) {
                        if (value instanceof Map && ((Map<?, ?>) value).containsKey("@type")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> mapValue = (Map<String, Object>) value;
                            value = serialize(getByName((String) mapValue.get("@type")), mapValue, true);
                        }
                        
                        if (value instanceof byte[]) {
                            byte[] bytes = (byte[]) value;
                            int bytesLen = bytes.length;
                            
                            if (bytesLen <= 253) {
                                buffer.put((byte) bytesLen);
                            } else {
                                buffer.put((byte) 0xFE);
                                buffer.put((byte) (bytesLen & 0xFF));
                                buffer.put((byte) ((bytesLen >> 8) & 0xFF));
                                buffer.put((byte) ((bytesLen >> 16) & 0xFF));
                            }
                            
                            buffer.put(bytes);
                            
                            int padding = (4 - ((bytesLen + (bytesLen <= 253 ? 1 : 4)) % 4)) % 4;
                            buffer.put(new byte[padding]);
                        }
                    } else if (type.equals("string")) {
                        byte[] bytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                        int bytesLen = bytes.length;
                        
                        if (bytesLen <= 253) {
                            buffer.put((byte) bytesLen);
                        } else {
                            buffer.put((byte) 0xFE);
                            buffer.put((byte) (bytesLen & 0xFF));
                            buffer.put((byte) ((bytesLen >> 8) & 0xFF));
                            buffer.put((byte) ((bytesLen >> 16) & 0xFF));
                        }
                        
                        buffer.put(bytes);
                        
                        int padding = (4 - ((bytesLen + (bytesLen <= 253 ? 1 : 4)) % 4)) % 4;
                        buffer.put(new byte[padding]);
                    }
                }
            } else {
                List<TLSchema> schemas = getByClassName(type);
                
                if (schemas != null && !schemas.isEmpty()) { // implicit
                    if (schemas.size() == 1) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> mapValue = (Map<String, Object>) value;
                        buffer.put(serialize(schemas.get(0), mapValue, true));
                    } else {
                        if (value instanceof byte[]) {
                            buffer.put((byte[]) value);
                        } else if (value instanceof Map && ((Map<?, ?>) value).containsKey("@type")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> mapValue = (Map<String, Object>) value;
                            buffer.put(serialize(getByName((String) mapValue.get("@type")), mapValue, true));
                        } else {
                            throw new TLError("Unknown value provided for implicit schemas " + schemas + ": " + value);
                        }
                    }
                } else { // explicit
                    if (type.startsWith("(")) {
                        String subtype = type.split(" ")[1].substring(0, type.split(" ")[1].length() - 1);
                        
                        if (type.contains("vector")) {
                            @SuppressWarnings("unchecked")
                            List<Object> list = (List<Object>) value;
                            buffer.putInt(list.size());
                            
                            for (Object item : list) {
                                buffer.put(serializeField(subtype, item));
                            }
                        }
                    } else {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> mapValue = (Map<String, Object>) value;
                        buffer.put(serialize(getByName(type), mapValue, false));
                    }
                }
            }
            
            byte[] result = new byte[buffer.position()];
            buffer.flip();
            buffer.get(result);
            return result;
        }
        
        public Object[] deserialize(byte[] data) {
            return deserialize(data, true, null);
        }
        
        public Object[] deserialize(byte[] data, boolean boxed, Map<String, String> args) {
            int i = 0;
            Map<String, Object> result = new HashMap<>();
            TLSchema schema = null;
            
            if (boxed) {
                if (data.length < 4) {
                    return new Object[] { data, data.length };
                }
                
                byte[] idBytes = Arrays.copyOfRange(data, 0, 4);
                schema = getById(idBytes, ByteOrder.LITTLE_ENDIAN);
                
                if (schema == null) {
                    return new Object[] { data, data.length };
                }
                
                result.put("@type", schema.getName());
                i += 4;
                args = schema.getArgs();
            }
            
            logger.log(Level.FINEST, "Deserializing schema with args " + args);
            
            if (args == null) {
                return new Object[] { result, i };
            }
            
            for (Map.Entry<String, String> entry : args.entrySet()) {
                String field = entry.getKey();
                String type = entry.getValue();
                
                if (type.contains("?")) {
                    int index = Integer.parseInt(type.substring(type.indexOf('.') + 1, type.indexOf('?')));
                    int flags = ((Number) result.getOrDefault("mode", result.getOrDefault("flags", 0))).intValue();
                    String binaryFlags = Integer.toBinaryString(flags);
                    
                    if (index >= binaryFlags.length() || binaryFlags.charAt(binaryFlags.length() - 1 - index) == '0') {
                        continue;
                    }
                    
                    type = type.split("\\?")[1];
                }
                
                if (i >= data.length) {
                    break;
                }
                
                if (BASE_TYPES.containsKey(type)) {
                    Integer byteLen = BASE_TYPES.get(type);
                    
                    if (byteLen != null) {
                        if (i + byteLen > data.length) {
                            break;
                        }
                        
                        if (type.equals("Bool")) {
                            byte[] boolBytes = Arrays.copyOfRange(data, i, i + byteLen);
                            if (Arrays.equals(boolBytes, new byte[]{(byte) 0x99, (byte) 0x72, (byte) 0x75, (byte) 0xb5})) {
                                result.put(field, true);
                            } else if (Arrays.equals(boolBytes, new byte[]{(byte) 0xbc, (byte) 0x79, (byte) 0x97, (byte) 0x37})) {
                                result.put(field, false);
                            }
                        } else if (type.equals("int128") || type.equals("int256")) {
                            result.put(field, TLGenerator.bytesToHex(Arrays.copyOfRange(data, i, i + byteLen)));
                        } else if (type.equals("int")) {
                            result.put(field, ByteBuffer.wrap(data, i, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
                        } else if (type.equals("long")) {
                            result.put(field, ByteBuffer.wrap(data, i, 8).order(ByteOrder.LITTLE_ENDIAN).getLong());
                        }
                        
                        i += byteLen;
                    } else {
                        if (type.equals("bytes") || type.equals("string")) {
                            if (i >= data.length) {
                                break;
                            }
                            
                            int byteLenVal;
                            int attachLen;
                            
                            if (data[i] == (byte) 0xFE) {
                                byteLenVal = (data[i + 1] & 0xFF) | ((data[i + 2] & 0xFF) << 8) | ((data[i + 3] & 0xFF) << 16);
                                attachLen = 4;
                                i += 4;
                            } else {
                                byteLenVal = data[i] & 0xFF;
                                attachLen = 1;
                                i += 1;
                            }
                            
                            if (i + byteLenVal > data.length) {
                                break;
                            }
                            
                            if (!autoDeserialize || 
                                (schema != null && 
                                 untouchables.containsKey(schema.getName()) && 
                                 untouchables.get(schema.getName()).contains(field))) {
                                result.put(field, Arrays.copyOfRange(data, i, i + byteLenVal));
                            } else {
                                try {
                                    Object[] temp = deserialize(Arrays.copyOfRange(data, i, i + byteLenVal));
                                    int j = (int) temp[1];
                                    
                                    if (j < byteLenVal) {
                                        List<Object> parts = new ArrayList<>();
                                        parts.add(temp[0]);
                                        
                                        while (j < byteLenVal) {
                                            temp = deserialize(Arrays.copyOfRange(data, i + j, i + byteLenVal));
                                            int jj = (int) temp[1];
                                            j += jj;
                                            
                                            if (jj == 0) {
                                                result.put(field, Arrays.copyOfRange(data, i, i + byteLenVal));
                                                break;
                                            }
                                            
                                            parts.add(temp[0]);
                                        }
                                        
                                        if (parts.size() > 1) {
                                            result.put(field, parts);
                                        } else {
                                            result.put(field, parts.get(0));
                                        }
                                    } else {
                                        result.put(field, temp[0]);
                                    }
                                } catch (Exception e) {
                                    result.put(field, Arrays.copyOfRange(data, i, i + byteLenVal));
                                }
                            }
                            
                            i += byteLenVal;
                            
                            int padding = (4 - ((byteLenVal + attachLen) % 4)) % 4;
                            i += padding;
                            
                            if (type.equals("string") && result.get(field) instanceof byte[]) {
                                result.put(field, new String((byte[]) result.get(field), StandardCharsets.UTF_8));
                            }
                        }
                    }
                } else {
                    if (type.startsWith("(")) {
                        String subtype = type.split(" ")[1].substring(0, type.split(" ")[1].length() - 1);
                        
                        if (type.contains("vector")) {
                            if (i + 4 > data.length) {
                                break;
                            }
                            
                            int length = ByteBuffer.wrap(data, i, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
                            i += 4;
                            
                            List<Object> list = new ArrayList<>();
                            TLSchema sch = getByName(subtype);
                            
                            for (int k = 0; k < length; k++) {
                                if (i >= data.length) {
                                    break;
                                }
                                
                                Object[] deser;
                                if (sch != null) {
                                    deser = deserialize(Arrays.copyOfRange(data, i, data.length), false, sch.getArgs());
                                } else {
                                    deser = deserialize(Arrays.copyOfRange(data, i, data.length));
                                }
                                
                                list.add(deser[0]);
                                i += (int) deser[1];
                            }
                            
                            result.put(field, list);
                        }
                    } else {
                        TLSchema sch = getByName(type);
                        
                        if (sch != null) {
                            Object[] deser = deserialize(Arrays.copyOfRange(data, i, data.length), false, sch.getArgs());
                            Map<String, Object> deserMap = (Map<String, Object>) deser[0];
                            deserMap.put("@type", sch.getName());
                            result.put(field, deserMap);
                            i += (int) deser[1];
                        } else {
                            Object[] deser = deserialize(Arrays.copyOfRange(data, i, data.length));
                            result.put(field, deser[0]);
                            i += (int) deser[1];
                        }
                    }
                }
            }
            
            return new Object[] { result, i };
        }
        
        public byte[] serialize(String schemaName, Map<String, Object> data, boolean boxed) {
            return serialize(getByName(schemaName), data, boxed);
        }
        
        public byte[] serialize(TLSchema schema, Map<String, Object> data) {
            return serialize(schema, data, true);
        }
    }
    
    /**
     * TL Registrator class
     */
    public static class TLRegistrator {
        private final Pattern re = Pattern.compile("\\s([^:]+):(\\(.+\\)|\\S+)");
        
        /**
         * Register a TL schema
         * @param schema Schema string
         * @return TLSchema instance
         */
        public TLSchema register(String schema) {
            schema = schema.split("//")[0];
            String name = schema.split(" ")[0];
            byte[] tlId;
            
            if (name.contains("#")) {
                String[] splitName = name.split("#");
                tlId = TLGenerator.hexToBytes(splitName[1]);
                name = splitName[0];
            } else {
                tlId = getId(schema.trim());
            }
            
            Map<String, String> args = splitFields(String.join(" ", Arrays.copyOfRange(schema.split(" "), 1, schema.split(" ").length - 1)));
            String className = schema.split(" ")[schema.split(" ").length - 1].replace(";", "");
            
            return new TLSchema(tlId, name, className, args);
        }
        
        /**
         * Split fields string into a map
         * @param fields Fields string
         * @return Map of field names to types
         */
        private Map<String, String> splitFields(String fields) {
            Map<String, String> result = new HashMap<>();
            StringBuilder temp = new StringBuilder();
            StringBuilder tempKey = new StringBuilder();
            int br = 0;
            
            for (char c : fields.toCharArray()) {
                if (c == '(') {
                    temp.append(c);
                    br++;
                    continue;
                }
                if (c == ')') {
                    temp.append(c);
                    br--;
                    continue;
                }
                if (c == ':' && br == 0) {
                    tempKey = new StringBuilder(temp.toString());
                    temp = new StringBuilder();
                    continue;
                }
                if (c == ' ' && br == 0) {
                    if (!tempKey.toString().isEmpty()) {
                        result.put(tempKey.toString(), temp.toString());
                    }
                    tempKey = new StringBuilder();
                    temp = new StringBuilder();
                    continue;
                }
                temp.append(c);
            }
            
            if (!tempKey.toString().isEmpty()) {
                result.put(tempKey.toString(), temp.toString());
            }
            
            return result;
        }
        
        /**
         * Clear schema string
         * @param schema Schema string
         * @return Cleared schema string
         */
        private String clear(String schema) {
            return schema.replace(";", "").replace("(", "").replace(")", "");
        }
        
        /**
         * Calculate CRC32 for schema
         * @param schema Schema string
         * @return CRC32 value
         */
        private int crc32(String schema) {
            CRC32 crc = new CRC32();
            crc.update(schema.getBytes(StandardCharsets.UTF_8));
            return (int) crc.getValue();
        }
        
        /**
         * Get ID for schema
         * @param schema Schema string
         * @return ID bytes
         */
        private byte[] getId(String schema) {
            return intToBytes(crc32(clear(schema)));
        }
    }
}
