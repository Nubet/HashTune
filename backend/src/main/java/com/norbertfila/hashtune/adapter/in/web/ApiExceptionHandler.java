package com.norbertfila.hashtune.adapter.in.web;

import com.norbertfila.hashtune.application.service.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ApiDtos.ProblemResponse> handle(ApplicationException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status()).body(new ApiDtos.ProblemResponse(
                "https://hashtune.local/problems/" + exception.code().toLowerCase(),
                exception.code(), exception.status().value(), exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiDtos.ProblemResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.internalServerError().body(new ApiDtos.ProblemResponse(
                "https://hashtune.local/problems/internal-error", "Internal Server Error", 500,
                "An unexpected error occurred", request.getRequestURI()));
    }
}
