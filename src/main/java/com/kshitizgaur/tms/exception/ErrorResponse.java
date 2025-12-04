package com.kshitizgaur.tms.exception;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;

/**
 * Standard error response structure for API error handling.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Builder.Default
    private List<FieldError> fieldErrors = new ArrayList<>();

    /**
     * Nested class for field-level validation errors.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }

    /**
     * Add a field error to the response.
     */
    public void addFieldError(String field, String message, Object rejectedValue) {
        if (fieldErrors == null) {
            fieldErrors = new ArrayList<>();
        }
        fieldErrors.add(FieldError.builder()
                .field(field)
                .message(message)
                .rejectedValue(rejectedValue)
                .build());
    }
}
