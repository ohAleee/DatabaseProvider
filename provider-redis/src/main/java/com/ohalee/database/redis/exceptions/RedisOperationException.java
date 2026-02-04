package com.ohalee.database.redis.exceptions;

import java.io.Serial;

/**
 * A generic runtime exception representing a failure during a Redis operation.
 * <p>
 * This exception serves as a wrapper for lower-level errors, such as:
 * <ul>
 * <li>Driver-specific errors (e.g., {@link io.lettuce.core.RedisException})</li>
 * <li>Connection failures</li>
 * <li>Transaction execution errors</li>
 * <li>Unexpected interruptions during command processing</li>
 * </ul>
 * <p>
 * Because it extends {@link RuntimeException}, it does not need to be declared in
 * method signatures, but it should be caught by top-level logic handling database interactions.
 */
public class RedisOperationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code RedisOperationException} with the specified detail message and cause.
     *
     * @param message The detail message (saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   The cause (saved for later retrieval by the {@link #getCause()} method).
     * (A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RedisOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}