package com.norbertfila.hashtune.exceptions;

import com.norbertfila.hashtune.dto.error.ProblemResponse;
import com.norbertfila.hashtune.exceptions.storage.StorageException;
import com.norbertfila.hashtune.exceptions.audio.AudioInputRejectedException;
import com.norbertfila.hashtune.exceptions.ApplicationException;
import com.norbertfila.hashtune.exceptions.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ProblemResponse> handle(ApplicationException exception, HttpServletRequest request) {
        return problem(
                exception.status(), exception.code(), exception.code(), exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    ResponseEntity<ProblemResponse> handleRateLimit(
            TooManyRequestsException exception, HttpServletRequest request) {
        ResponseEntity<ProblemResponse> response = problem(
                exception.status(), exception.code(), exception.code(), exception.getMessage(), request, Map.of());
        response.getHeaders()
                .add(
                        "Retry-After",
                        Long.toString(Math.max(1, exception.retryAfter().toSeconds())));
        return response;
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemResponse> handleAccessDenied(
            AccessDeniedException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "FORBIDDEN",
                "You do not have permission to access this resource.",
                request,
                Map.of());
    }

    @ExceptionHandler(AudioInputRejectedException.class)
    ResponseEntity<ProblemResponse> handleAudioInputRejected(
            AudioInputRejectedException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Invalid Audio",
                exception.code(),
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(StorageException.class)
    ResponseEntity<ProblemResponse> handleStorage(StorageException exception, HttpServletRequest request) {
        log.error("Storage operation failed on {}", request.getRequestURI(), exception);
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Storage Unavailable",
                "STORAGE_UNAVAILABLE",
                "Audio storage is temporarily unavailable. Try again later.",
                request,
                Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ProblemResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File Too Large",
                "FILE_TOO_LARGE",
                "The uploaded audio file exceeds the maximum allowed size.",
                request,
                Map.of("file", "File is too large."));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ProblemResponse> handleMissingPart(
            MissingServletRequestPartException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Missing File",
                "MISSING_FILE",
                "Multipart field 'file' is required.",
                request,
                Map.of("file", "File is required."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ProblemResponse> handleMissingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Missing Parameter",
                "MISSING_PARAMETER",
                "Request parameter '%s' is required.".formatted(exception.getParameterName()),
                request,
                Map.of(exception.getParameterName(), "Parameter is required."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        String parameter = exception.getName();
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid Parameter",
                "INVALID_PARAMETER",
                "Request parameter '%s' has an invalid value.".formatted(parameter),
                request,
                Map.of(parameter, "Value is invalid."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemResponse> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() == null
                                ? "Value is invalid."
                                : fieldError.getDefaultMessage(),
                        (first, ignored) -> first));
        return problem(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "VALIDATION_FAILED",
                "One or more request values are invalid.",
                request,
                errors);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported Media Type",
                "UNSUPPORTED_MEDIA_TYPE",
                "The request content type is not supported.",
                request,
                Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled API error on {}", request.getRequestURI(), exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "INTERNAL_ERROR",
                "The server could not complete the request.",
                request,
                Map.of());
    }

    private ResponseEntity<ProblemResponse> problem(
            HttpStatus status,
            String title,
            String code,
            String detail,
            HttpServletRequest request,
            Map<String, String> errors) {
        return ResponseEntity.status(status)
                .body(new ProblemResponse(
                        "https://hashtune.local/problems/" + code.toLowerCase(),
                        title,
                        status.value(),
                        detail,
                        request.getRequestURI(),
                        code,
                        errors.isEmpty() ? null : errors));
    }
}
