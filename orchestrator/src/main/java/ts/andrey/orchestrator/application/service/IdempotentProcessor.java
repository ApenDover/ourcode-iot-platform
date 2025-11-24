package ts.andrey.orchestrator.application.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ts.andrey.orchestrator.application.port.RedisDataServicePort;
import ts.andrey.orchestrator.dto.ApiV1DevicesDeviceIdVersionPost200Response;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdempotentProcessor {

    @Value("${orchestrator.redis.enabled}")
    private Boolean enabled;

    private final RedisDataServicePort redisDataServicePort;

    public Optional<ApiV1DevicesDeviceIdVersionPost200Response> checkIdempotentKey(String idempotentKey) {
        if (!BooleanUtils.toBoolean(enabled)) {
            return Optional.empty();
        }
        return redisDataServicePort.getResponse(idempotentKey);
    }

    public void saveResponse(String idempotencyKey, ApiV1DevicesDeviceIdVersionPost200Response response) {
        if (!BooleanUtils.toBoolean(enabled)) {
            return;
        }
        redisDataServicePort.saveResponse(idempotencyKey, response);
    }

}
