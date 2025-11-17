package ts.andrey.orchestrator.infrastructure.util;

import com.fasterxml.jackson.databind.JsonNode;
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
            if (payload instanceof String stringPayload) {
                return handleStringPayload(stringPayload);
            }

            final var map = OBJECT_MAPPER.convertValue(payload, Map.class);
            return mapToStructSimple(map);

        } catch (Exception e) {
            throw new RuntimeException("Error converting Object to Struct", e);
        }
    }

    private Struct handleStringPayload(String payload) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(payload);
            if (jsonNode.isObject()) {
                final var map = OBJECT_MAPPER.convertValue(jsonNode, Map.class);
                return mapToStructSimple(map);
            }
        } catch (Exception _) {
        }

        return Struct.newBuilder()
                .putFields("updateVersion", Value.newBuilder().setStringValue(payload).build())
                .build();
    }

    private Struct mapToStructSimple(Map<String, Object> map) {
        Struct.Builder builder = Struct.newBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Value value = convertToValueSimple(entry.getValue());
            builder.putFields(entry.getKey(), value);
        }
        return builder.build();
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

}
