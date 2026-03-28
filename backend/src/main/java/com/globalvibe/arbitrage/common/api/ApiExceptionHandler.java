package com.globalvibe.arbitrage.common.api;

import com.globalvibe.arbitrage.domain.analysis.service.AnalysisResultNotFoundException;
import com.globalvibe.arbitrage.domain.search.service.QueryRewriteNotFoundException;
import com.globalvibe.arbitrage.domain.product.service.ProductNotFoundException;
import com.globalvibe.arbitrage.domain.report.service.ReportNotFoundException;
import com.globalvibe.arbitrage.domain.task.service.AnalysisTaskNotFoundException;
import com.globalvibe.arbitrage.domain.task.service.InvalidTaskContextException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AnalysisTaskNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTaskNotFound(AnalysisTaskNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure("TASK_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleReportNotFound(ReportNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure("REPORT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure("PRODUCT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AnalysisResultNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAnalysisNotFound(AnalysisResultNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure("ANALYSIS_RESULT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(QueryRewriteNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleQueryRewriteNotFound(QueryRewriteNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure("QUERY_REWRITE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTaskContextException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTaskContext(InvalidTaskContextException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("INVALID_TASK_CONTEXT", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("VALIDATION_ERROR", ex.getMessage()));
    }
}
