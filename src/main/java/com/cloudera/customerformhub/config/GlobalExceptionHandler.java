package com.cloudera.customerformhub.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

// Catches exceptions thrown by any controller and returns a clean JSON response
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex
    ) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", ex.getStatusCode().value(),
                "error", "Unprocessable Entity",
                "message", ex.getReason() == null
                        ? "The uploaded file could not be processed."
                        : ex.getReason()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    // Handles any unexpected error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception ex) {
        // Log the real error on the server (for the developer)
        System.err.println("Unexpected error: " + ex.getMessage());

        // Return a clean, safe message to the client (no stack trace)
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 500,
                "error", "Internal Server Error",
                "message", "Something went wrong while processing your request."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // Handles bad input (e.g. illegal argument)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadInput(IllegalArgumentException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
