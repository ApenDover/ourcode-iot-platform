package ts.andrey.orchestrator.infrastructure.handler;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ts.andrey.orchestrator.domain.exception.DeviceServiceRollbackException;
import ts.andrey.orchestrator.domain.exception.ExceptionMessage;
import ts.andrey.orchestrator.domain.exception.RouterManagerRollbackException;
import ts.andrey.orchestrator.dto.ApiV1DevicesDeviceIdVersionPost502Response;

import java.util.Arrays;

@Slf4j
@RestControllerAdvice
public class DefaultExceptionHandler {

    private static final String INSTANCE_NAME = "ORCHESTRATOR";

    @ExceptionHandler(value = RouterManagerRollbackException.class)
    public ResponseEntity<ApiV1DevicesDeviceIdVersionPost502Response> handleException(
            RouterManagerRollbackException ex
    ) {
        final var errorResult = new ApiV1DevicesDeviceIdVersionPost502Response();
        errorResult.setError(ExceptionMessage.ROUTER_MANAGER_FAILED.name());
        errorResult.setCompensated(true);
        errorResult.setDetails(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(errorResult);
    }

    @ExceptionHandler(value = DeviceServiceRollbackException.class)
    public ResponseEntity<ApiV1DevicesDeviceIdVersionPost502Response> handleException(
            DeviceServiceRollbackException ex
    ) {
        final var errorResult = new ApiV1DevicesDeviceIdVersionPost502Response();
        errorResult.setError(ExceptionMessage.ROUTER_MANAGER_FAILED.name());
        errorResult.setCompensated(false);
        errorResult.setDetails(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(errorResult);
    }

    @ExceptionHandler(value = FeignException.class)
    public ResponseEntity<ts.andrey.orchestrator.dto.Error> handleException(FeignException ex) {
        log.error(ExceptionMessage.FEIGN_INTEGRATION_FAILED.name(), ex);

        final var trace = String.valueOf(Arrays.stream(ex.getStackTrace())
                .findFirst()
                .orElse(null));

        final var errorResult = new ts.andrey.orchestrator.dto.Error();
        errorResult.setInstance(INSTANCE_NAME);
        errorResult.setDetail(ex.getMessage());
        errorResult.setTitle(ex.getLocalizedMessage());
        errorResult.setTrace(trace);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResult);
    }

}
