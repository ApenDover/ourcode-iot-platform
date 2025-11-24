package ts.andrey.orchestrator.infrastructure.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String minifyJson(String jsonString) {
        try {
            final var jsonNode = OBJECT_MAPPER.readTree(jsonString);
            return OBJECT_MAPPER.disable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(jsonNode);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return jsonString;
        }
    }

}
