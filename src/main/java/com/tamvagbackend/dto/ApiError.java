package com.tamvagbackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,

        int status,

        String error,

        String message,

        String path,

        @JsonProperty("request_id")
        String requestId,

        List<FieldError> details
) {

    public record FieldError(
            String field,
            String message
    ) {}
}