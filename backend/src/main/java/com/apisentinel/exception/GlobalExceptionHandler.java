package com.apisentinel.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        // @Valid fail hua
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                List<ApiError.FieldViolation> violations = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(e -> new ApiError.FieldViolation(e.getField(), e.getDefaultMessage()))
                                .toList();

                ApiError body = ApiError.of(
                                HttpStatus.BAD_REQUEST.value(),
                                "Validation Failed",
                                "One or more fields are invalid",
                                request.getRequestURI(),
                                violations);

                return ResponseEntity.badRequest().body(body);
        }

        // Malformed JSON ya galat enum value (jaise "method": "BANANA")
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex,
                        HttpServletRequest request) {
                ApiError body = ApiError.of(
                                HttpStatus.BAD_REQUEST.value(),
                                "Malformed Request",
                                "Request body is missing, malformed, or contains an invalid value",
                                request.getRequestURI());

                return ResponseEntity.badRequest().body(body);
        }

        // Resource nahi mila
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex,
                        HttpServletRequest request) {
                ApiError body = ApiError.of(
                                HttpStatus.NOT_FOUND.value(),
                                "Not Found",
                                ex.getMessage(),
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiError> misMatchType(MethodArgumentTypeMismatchException ex,
                        HttpServletRequest request) {
                ApiError body = ApiError.of(
                                HttpStatus.BAD_REQUEST.value(),
                                "mismatch value",
                                ex.getMessage(),
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        @ExceptionHandler(InvalidRequestException.class)
        public ResponseEntity<ApiError> handleInvalidRequest(InvalidRequestException ex,
                        HttpServletRequest request) {
                ApiError body = ApiError.of(
                                HttpStatus.BAD_REQUEST.value(),
                                "Invalid Request",
                                ex.getMessage(),
                                request.getRequestURI());

                return ResponseEntity.badRequest().body(body);
        }

        // Baaki sab kuch (unexpected bugs)
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
                log.error("Unexpected error on {}", request.getRequestURI(), ex);

                ApiError body = ApiError.of(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Internal Server Error",
                                "Something went wrong",
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }

        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
                ApiError body = ApiError.of(
                                HttpStatus.CONFLICT.value(),
                                "Conflict",
                                ex.getMessage(),
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        @ExceptionHandler(AuthenticationFailedException.class)
        public ResponseEntity<ApiError> handleAuthFailed(AuthenticationFailedException ex,
                        HttpServletRequest request) {
                ApiError body = ApiError.of(
                                HttpStatus.UNAUTHORIZED.value(),
                                "Unauthorized",
                                ex.getMessage(),
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .header("WWW-Authenticate", "Bearer")
                                .body(body);
        }

        @ExceptionHandler(TooManyRequestsException.class)
        public ResponseEntity<ApiError> handleTooMany(TooManyRequestsException ex,
                        HttpServletRequest request) {
                ApiError body = ApiError.of(
                                HttpStatus.TOO_MANY_REQUESTS.value(),
                                "Too Many Requests",
                                ex.getMessage(),
                                request.getRequestURI());

                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                                .body(body);
        }
}
