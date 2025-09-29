package ts.andrey.eventservice.exception;

import com.nashkod.avro.DeviceError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EventServiceException.class)
    public ResponseEntity<DeviceError> handleEntityNotFound(EventServiceException ex, HttpServletRequest request) {
        log.error(ex.getMessage(), ex);
        return buildDeviceError(
                HttpStatus.NOT_FOUND,
                "Entity not found",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<DeviceError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.error(ex.getMessage(), ex);
        return buildDeviceError(
                HttpStatus.BAD_REQUEST,
                "Invalid argument",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DeviceError> handleException(Exception ex, HttpServletRequest request) {
        log.error(ex.getMessage(), ex);
        return buildDeviceError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DeviceServiceException.class)
    public ResponseEntity<DeviceError> handleException(DeviceServiceException ex, HttpServletRequest request) {
        log.error(ex.getMessage(), ex);
        return buildDeviceError(
                ex.getStatus(),
                "Process exception",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler({
            DataAccessException.class,
            JpaSystemException.class,
            SQLGrammarException.class,
            ConstraintViolationException.class,
            TransactionSystemException.class
    })
    public ResponseEntity<DeviceError> handleDatabaseExceptions(Exception ex, WebRequest request) {
        log.error(ex.getMessage(), ex);

        return buildDeviceError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Database Error",
                ex.getMessage(),
                request.getDescription(false)
        );
    }

    private ResponseEntity<DeviceError> buildDeviceError(
            HttpStatus status, String title,
            String detail, String instance
    ) {
        final var error = new DeviceError();
        error.setTitle(title);
        error.setStatus(status.value());
        error.setDetail(detail);
        error.setInstance(instance);
        error.setTrace(MDC.get("trace-id"));
        return ResponseEntity.status(status).body(error);
    }
    
}
