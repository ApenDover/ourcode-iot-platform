package ts.andrey.eventservice.exception;

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
import ts.andrey.eventservice.config.MdcInterceptor;
import ts.andrey.eventservice.model.ResponseError;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EventServiceException.class)
    public ResponseEntity<ResponseError> handleEntityNotFound(EventServiceException ex,
                                                              HttpServletRequest request) {
        log.info(ex.getMessage(), ex);
        return buildDeviceError(
                HttpStatus.NOT_FOUND,
                "Entity not found",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseError> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest request) {
        log.error(ex.getMessage(), ex);
        return buildDeviceError(
                HttpStatus.BAD_REQUEST,
                "Invalid argument",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseError> handleException(Exception ex, HttpServletRequest request) {
        log.error(ex.getMessage(), ex);
        return buildDeviceError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler({
            DataAccessException.class,
            ConstraintViolationException.class,
            TransactionSystemException.class
    })
    public ResponseEntity<ResponseError> handleDatabaseExceptions(Exception ex, WebRequest request) {
        log.error(ex.getMessage(), ex);

        return buildDeviceError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Database Error",
                ex.getMessage(),
                request.getDescription(false)
        );
    }

    private ResponseEntity<ResponseError> buildDeviceError(
            HttpStatus status, String title,
            String detail, String instance
    ) {
        final var error = ResponseError.builder()
                .title(title)
                .status(status.value())
                .detail(detail)
                .instance(instance)
                .trace(MDC.get(MdcInterceptor.TRACE))
                .build();
        return ResponseEntity.status(status).body(error);
    }

}
