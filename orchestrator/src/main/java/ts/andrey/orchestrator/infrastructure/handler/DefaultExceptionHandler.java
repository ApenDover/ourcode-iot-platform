package ts.andrey.orchestrator.infrastructure.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ts.andrey.orchestrator.domain.exception.DeviceServiceRollbackException;
import ts.andrey.orchestrator.domain.exception.ExceptionMessage;
import ts.andrey.orchestrator.domain.exception.RouterManagerRollbackException;
import ts.andrey.orchestrator.dto.ApiV1DevicesDeviceIdVersionPost502Response;

@Slf4j
@RestControllerAdvice
public class DefaultExceptionHandler {

    @ExceptionHandler(value = RouterManagerRollbackException.class)
    public ResponseEntity<ApiV1DevicesDeviceIdVersionPost502Response> handleException(RouterManagerRollbackException ex) {
        final var errorResult = new ApiV1DevicesDeviceIdVersionPost502Response();
        errorResult.setError(ExceptionMessage.ROUTER_MANAGER_FAILED.name());
        errorResult.setCompensated(true);
        errorResult.setDetails(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(errorResult);
    }

    @ExceptionHandler(value = DeviceServiceRollbackException.class)
    public ResponseEntity<ApiV1DevicesDeviceIdVersionPost502Response> handleException(DeviceServiceRollbackException ex) {
        final var errorResult = new ApiV1DevicesDeviceIdVersionPost502Response();
        errorResult.setError(ExceptionMessage.ROUTER_MANAGER_FAILED.name());
        errorResult.setCompensated(false);
        errorResult.setDetails(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(errorResult);
    }

}
