package ts.andrey.orchestrator.infrastructure.adapter.out;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import ts.andrey.orchestrator.application.port.RedisDataServicePort;
import ts.andrey.orchestrator.domain.metrics.OrchestratorMetrics;
import ts.andrey.orchestrator.dto.ApiV1DevicesDeviceIdVersionPost200Response;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDataServiceAdapter implements RedisDataServicePort {

    @Value("${orchestrator.redis.ttl-minutes}")
    private int ttlMinutes;

    private final RedisTemplate<String, ApiV1DevicesDeviceIdVersionPost200Response> redisTemplate;
    private final OrchestratorMetrics orchestratorMetrics;

    @Override
    @WithSpan("RedisGet")
    public Optional<ApiV1DevicesDeviceIdVersionPost200Response> getResponse(String idempotentKey) {
        try {
            final var response = redisTemplate.opsForValue().get(idempotentKey);
            if (Objects.nonNull(response)) {
                log.info("Response найден в REDIS: {}", response);
            }
            return Optional.ofNullable(response);
        } catch (Exception e) {
            orchestratorMetrics.orchestratorRedisFailure();
            log.error("Redis недоступен", e);
        }
        return Optional.empty();
    }

    @Override
    @WithSpan("RedisSave")
    public void saveResponse(String idempotentKey, ApiV1DevicesDeviceIdVersionPost200Response response) {
        try {
            redisTemplate.opsForValue()
                    .set(idempotentKey, response, ttl());
            orchestratorMetrics.orchestratorRedisSuccess();
        } catch (Exception e) {
            orchestratorMetrics.orchestratorRedisFailure();
            log.error("Redis недоступен", e);
        }
    }

    private Duration ttl() {
        return Duration.ofMinutes(ttlMinutes);
    }

}
