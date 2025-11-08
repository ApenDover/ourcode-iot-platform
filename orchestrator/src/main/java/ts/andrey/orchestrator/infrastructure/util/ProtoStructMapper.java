package ts.andrey.orchestrator.infrastructure.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class ProtoStructMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public Struct toStruct(Object payload) {
        if (payload == null) {
            return Struct.getDefaultInstance();
        }

        try {
            final var map = OBJECT_MAPPER.convertValue(payload, Map.class);
            return mapToStructSimple(map);

        } catch (Exception e) {
            throw new RuntimeException("Error converting Object to Struct", e);
        }
    }

    public Object fromStruct(Struct struct) {
        if (struct == null || struct.getFieldsCount() == 0) {
            return null;
        }

        try {
            Map<String, Object> map = structToMapSimple(struct);
            return OBJECT_MAPPER.convertValue(map, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Error converting Struct to Object", e);
        }
    }

    private Struct mapToStructSimple(Map<String, Object> map) {
        Struct.Builder builder = Struct.newBuilder();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Value value = convertToValueSimple(entry.getValue());
            builder.putFields(entry.getKey(), value);
        }

        return builder.build();
    }

    private static Map<String, Object> structToMapSimple(Struct struct) {
        return struct.getFieldsMap().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> convertFromValueSimple(entry.getValue())
                ));
    }

    private Value convertToValueSimple(Object obj) {
        return switch (obj) {
            case null -> Value.newBuilder().setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build();
            case String s -> Value.newBuilder().setStringValue(s).build();
            case Boolean b -> Value.newBuilder().setBoolValue(b).build();
            case Number number -> Value.newBuilder().setNumberValue(number.doubleValue()).build();
            default -> Value.newBuilder().setStringValue(obj.toString()).build();
        };
    }

    private static Object convertFromValueSimple(Value value) {
        return switch (value.getKindCase()) {
            case STRING_VALUE -> value.getStringValue();
            case NUMBER_VALUE -> value.getNumberValue();
            case BOOL_VALUE -> value.getBoolValue();
            default -> new Object();
        };
    }

}
