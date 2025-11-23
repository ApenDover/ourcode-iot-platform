package ts.andrey.orchestrator.application.outport;

import ts.andrey.orchestrator.dto.ApiV1DevicesDeviceIdVersionPost200Response;

import java.util.Optional;

public interface RedisDataServicePort {

    Optional<ApiV1DevicesDeviceIdVersionPost200Response> getResponse(String idempotentKey);

    void saveResponse(String idempotentKey, ApiV1DevicesDeviceIdVersionPost200Response response);

}
