package com.creedpetitt.aiservicesbackend.exception;

import com.creedpetitt.aiservicesbackend.dto.ErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto> handleResponseStatusException(ResponseStatusException ex) {
        logger.error("ResponseStatusException: status={}, reason={}", ex.getStatusCode(), ex.getReason(), ex);
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                ex.getReason(),
                ex.getStatusCode().value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex) {
        logger.error("Unexpected exception: {}", ex.getMessage(), ex);
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                "An unexpected error occurred", // Avoid leaking internal implementation details
                500,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
