package ts.andrey.orchestrator.testutils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

@UtilityClass
public class ReadJsonFileUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SneakyThrows
    public String readStringFromFile(String fileName) {
        final var resource = new ClassPathResource(fileName);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    public <T> List<T> readDtoFromFile(String fileName, Class<T> tClass) {
        try {
            final var json = readStringFromFile(fileName);
            final var type = OBJECT_MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, tClass);
            return OBJECT_MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
