package com.smartlogix.pricefinder.exception;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(PriceFinderException.class)
    public ResponseEntity<Map<String, Object>> handlePriceFinder(PriceFinderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(400, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Solicitud inválida.");
        return ResponseEntity.badRequest().body(body(400, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handle(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(500, "No fue posible consultar precios reales en este momento: " + ex.getMessage()));
    }

    private Map<String, Object> body(int status, String message) {
        return Map.of("timestamp", OffsetDateTime.now().toString(), "status", status, "message", message);
    }
}
