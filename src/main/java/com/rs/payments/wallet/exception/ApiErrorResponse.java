package com.rs.payments.wallet.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Uniform error response body returned by {@link ApiExceptionHandler}
 * for every non-2xx HTTP response produced by this service.
 *
 * Example JSON:
 * {
 *   "timestamp": "2024-01-28T14:32:00",
 *   "status":    409,
 *   "error":     "Conflict",
 *   "message":   "Username or email already exists",
 *   "path":      "/users"
 * }
 */
@Schema(description = "Standard error response envelope")
public record ApiErrorResponse(

        @Schema(description = "Time the error occurred",
                example = "2024-01-28T14:32:00")
        LocalDateTime timestamp,

        @Schema(description = "HTTP status code",
                example = "409")
        int status,

        @Schema(description = "HTTP reason phrase matching the status code",
                example = "Conflict")
        String error,

        @Schema(description = "Human-readable description of what went wrong",
                example = "Username or email already exists")
        String message,

        @Schema(description = "The request path that triggered this error",
                example = "/users")
        String path
) {}