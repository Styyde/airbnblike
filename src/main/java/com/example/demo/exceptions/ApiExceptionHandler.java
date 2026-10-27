package com.example.demo.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<?> imageSize(HttpServletRequest request) { return error(HttpStatus.PAYLOAD_TOO_LARGE, "Chaque photo doit peser au maximum 5 Mo.", request); }
    private ResponseEntity<?> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now().toString(),
                "status", status.value(), "message", message, "path", request.getRequestURI()));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> domain(ApiException ex, HttpServletRequest request) { return error(ex.getStatus(), ex.getMessage(), request); }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> forbidden(HttpServletRequest request) { return error(HttpStatus.FORBIDDEN, "Accès interdit", request); }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> unauthorized(HttpServletRequest request) { return error(HttpStatus.UNAUTHORIZED, "Identifiants invalides", request); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).sorted().collect(java.util.stream.Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<?> invalid(HttpServletRequest request) { return error(HttpStatus.BAD_REQUEST, "Requête invalide", request); }

    @ExceptionHandler({DataIntegrityViolationException.class, TransientDataAccessException.class})
    public ResponseEntity<?> conflict(HttpServletRequest request) { return error(HttpStatus.CONFLICT, "Conflit de données ; actualisez puis réessayez", request); }
}
