package com.example.demo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            InvalidRequestException ex, WebRequest request) {
        log.error("Invalid request: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(LLMServiceException.class)
    public ResponseEntity<ErrorResponse> handleLLMServiceException(
            LLMServiceException ex, WebRequest request) {
        log.error("LLM service error: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("LLM Service Error")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSizeException(
            MaxUploadSizeExceededException ex, WebRequest request) {
        log.error("File upload size exceeded: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .error("File Too Large")
                .message("File size exceeds maximum limit of 50MB")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccessException(
            ResourceAccessException ex, WebRequest request) {
        String rootCause = ex.getCause() != null ? ex.getCause().getClass().getSimpleName() : "Unknown";
        log.warn("LLM service I/O error ({}): {}", rootCause, ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("Could not reach the LLM service. Please try again in a moment.")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(
            IOException ex, WebRequest request) {
        log.error("File processing error: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("File Processing Error")
                .message("Unable to process file. Please ensure it's a valid PDF, DOCX, or TXT file.")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> handleRestClientException(
            RestClientException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        Throwable cause = ex.getCause();
        String causeName = cause != null ? cause.getClass().getSimpleName() : "Unknown";

        if (cause instanceof SocketTimeoutException) {
            log.warn("LLM read timeout ({}): {}", causeName, ex.getMessage());
            ErrorResponse error = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.GATEWAY_TIMEOUT.value())
                    .error("Gateway Timeout")
                    .message("The LLM took too long to respond. It was retried or fallback handled.")
                    .path(path)
                    .build();
            return new ResponseEntity<>(error, HttpStatus.GATEWAY_TIMEOUT);
        }

        log.error("Rest client error while calling LLM: {}", ex.getMessage(), ex);
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_GATEWAY.value())
                .error("Bad Gateway")
                .message("An error occurred while calling the LLM service.")
                .path(path)
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            NoResourceFoundException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        
        // Filter out Chrome DevTools and favicon requests
        if (path.contains("/.well-known/") || path.contains("favicon.ico") || 
            path.contains("com.chrome.devtools")) {
            return ResponseEntity.notFound().build();
        }
        
        log.warn("Resource not found: {}", path);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message("The requested resource was not found")
                .path(path)
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        
        // Filter out Chrome DevTools requests from global exception logging
        if (path.contains("/.well-known/") || path.contains("com.chrome.devtools")) {
            log.debug("Filtered Chrome DevTools request: {}", path);
            return ResponseEntity.notFound().build();
        }
        
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .path(path)
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Add this method inside the GlobalExceptionHandler class
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleAsyncRequestTimeoutException(
            AsyncRequestTimeoutException ex, WebRequest request) {

        log.error("Request timed out waiting for the AI provider response.");

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.GATEWAY_TIMEOUT.value())
                .error("Gateway Timeout")
                .message("The AI provider took too long to respond. The model might be experiencing high traffic. Please try again.")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(error, HttpStatus.GATEWAY_TIMEOUT);
    }
}
