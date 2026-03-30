package TrackTogether.webapi.exception;

import TrackTogether.webapi.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.logging.Logger;

// Global exception handler for all API controllers
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = Logger.getLogger(ApiExceptionHandler.class.getName());

    // Handle ResponseStatusException
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request) {

        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();

        ApiError error = new ApiError(
                ex.getStatusCode().value(),
                message,
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    // Handle invalid input (bad request)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Handle all unexpected errors (fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request) {


        logger.severe(ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}