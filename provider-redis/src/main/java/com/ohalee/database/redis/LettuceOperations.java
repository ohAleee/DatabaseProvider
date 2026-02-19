package com.ohalee.database.redis;

import com.ohalee.database.redis.builtin.StringRedisOperations;
import com.ohalee.database.redis.exceptions.RedisOperationException;
import io.lettuce.core.RedisException;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Enhanced Redis operations wrapper using the Lettuce driver.
 * <p>
 * This class abstracts the management of Redis connections for Synchronous, Asynchronous,
 * and Transactional operations.
 * </p>
 * <p>
 * <strong>Important Implementation Note:</strong> The {@link #connection()} method is expected
 * to return a <em>new</em> or <em>pooled</em> connection. This class calls {@code close()}
 * on the connection after every operation. If a shared/singleton connection is returned,
 * this class will close it, rendering the application unable to communicate with Redis.
 * </p>
 *
 * @param <K> Key type (e.g., String)
 * @param <V> Value type (e.g., String)
 * @see StringRedisOperations
 * @see <a href="https://lettuce.io/core/release/api/">Lettuce API Documentation</a>
 */
public abstract class LettuceOperations<K, V> {

    /**
     * Provides a dedicated Redis connection for the current operation.
     * <p>
     * The connection provided here will be closed (or returned to the pool)
     * automatically after the operation completes.
     *
     * @return a StatefulRedisConnection instance.
     */
    protected abstract StatefulRedisConnection<K, V> connection();

    /**
     * Executes an asynchronous Redis operation.
     * <p>
     * The connection is automatically closed when the future completes (successfully or exceptionally).
     * Connection acquisition is separated from operation execution so that failure messages
     * accurately reflect where the error occurred.
     *
     * @param operation the operation to execute via {@link RedisAsyncCommands}.
     * @param <T>       the type of the result.
     * @return a CompletableFuture representing the result of the operation.
     */
    public <T> CompletableFuture<T> executeAsync(AsyncRedisOperation<K, V, T> operation) {
        StatefulRedisConnection<K, V> connection;
        try {
            connection = this.connection();
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new RedisOperationException("Failed to acquire Redis connection", e));
        }

        try {
            CompletableFuture<T> result = operation.execute(connection.async());

            if (result == null) {
                connection.close();
                return CompletableFuture.completedFuture(null);
            }

            return result.whenComplete((res, ex) -> connection.close());
        } catch (Exception e) {
            connection.close();
            return CompletableFuture.failedFuture(new RedisOperationException("Redis operation failed", e));
        }
    }

    /**
     * Executes a synchronous Redis operation using try-with-resources.
     *
     * @param operation the operation to execute via {@link RedisCommands}.
     * @param <T>       the type of the result.
     * @return the result of the operation.
     * @throws RedisOperationException if a Redis-specific error or general exception occurs.
     */
    public <T> T executeSync(SyncRedisOperation<K, V, T> operation) {
        try (StatefulRedisConnection<K, V> connection = this.connection()) {
            return operation.execute(connection.sync());
        } catch (RedisException e) {
            throw new RedisOperationException("Redis driver error during sync operation", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisOperationException("Unexpected error during sync operation", e);
        }
    }

    /**
     * Executes a synchronous transaction (MULTI ... EXEC).
     * <p>
     * This method guarantees that {@code DISCARD} is called if an error occurs
     * during the queuing phase (before {@code EXEC}). If {@code EXEC} itself fails,
     * the transaction is already concluded and {@code DISCARD} is not called.
     *
     * @param operation the operation to execute within the transaction context.
     * @return the TransactionResult containing the results of all queued commands.
     * @throws RedisOperationException if the transaction fails.
     */
    public TransactionResult executeTransactionSync(SyncRedisOperation<K, V, Void> operation) {
        try (StatefulRedisConnection<K, V> connection = this.connection()) {
            RedisCommands<K, V> sync = connection.sync();
            boolean execCalled = false;
            try {
                sync.multi();
                operation.execute(sync);
                execCalled = true;
                return sync.exec();
            } catch (Exception e) {
                if (!execCalled) {
                    try {
                        sync.discard();
                    } catch (Exception discardEx) {
                        e.addSuppressed(discardEx);
                    }
                }
                throw new RedisOperationException("Redis sync transaction failed", e);
            }
        }
    }

    /**
     * Executes an asynchronous transaction (MULTI ... EXEC).
     * <p>
     * {@code DISCARD} is only called if the failure occurs during the queuing phase
     * (before {@code EXEC}). If {@code EXEC} itself fails, the transaction is already
     * concluded and {@code DISCARD} is not issued.
     * Connection acquisition is guarded so a failure there returns a failed future
     * rather than leaking a connection.
     *
     * @param operation the operation to execute within a transaction.
     * @return a CompletableFuture containing the TransactionResult.
     */
    public CompletableFuture<TransactionResult> executeTransactionAsync(AsyncRedisOperation<K, V, Void> operation) {
        StatefulRedisConnection<K, V> connection;
        try {
            connection = this.connection();
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new RedisOperationException("Failed to acquire Redis connection", e));
        }

        RedisAsyncCommands<K, V> async = connection.async();

        try {
            return async.multi()
                    .thenCompose(ok -> {
                        try {
                            return operation.execute(async)
                                    .thenCompose(s -> async.exec());
                        } catch (Exception e) {
                            // Queuing phase failed — DISCARD is valid here
                            return async.discard()
                                    .whenComplete((s, discardEx) -> {
                                        if (discardEx != null) e.addSuppressed(discardEx);
                                    })
                                    .thenCompose(s -> CompletableFuture.failedFuture(e));
                        }
                    })
                    .handle((result, ex) -> {
                        try {
                            if (ex != null) {
                                // Reached if queuing failed (after DISCARD) or if exec() failed
                                // In both cases the transaction is already concluded
                                Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
                                throw new CompletionException(new RedisOperationException("Async transaction failed", cause));
                            }
                            return result;
                        } finally {
                            connection.close();
                        }
                    })
                    .toCompletableFuture();
        } catch (Exception e) {
            connection.close();
            return CompletableFuture.failedFuture(new RedisOperationException("Redis operation failed", e));
        }
    }

    /**
     * Functional interface for asynchronous Redis operations.
     *
     * @param <K> Key Type
     * @param <V> Value Type
     * @param <T> Result Type
     */
    @FunctionalInterface
    public interface AsyncRedisOperation<K, V, T> {
        /**
         * Executes logic against the Async commands interface.
         *
         * @param async The async command interface.
         * @return A future representing the result.
         * @throws Exception Allows implementing methods to throw exceptions without strict try/catch blocks.
         */
        CompletableFuture<T> execute(RedisAsyncCommands<K, V> async) throws Exception;
    }

    /**
     * Functional interface for synchronous Redis operations.
     *
     * @param <K> Key Type
     * @param <V> Value Type
     * @param <T> Result Type
     */
    @FunctionalInterface
    public interface SyncRedisOperation<K, V, T> {
        /**
         * Executes logic against the Sync commands interface.
         *
         * @param sync The sync command interface.
         * @return The result.
         * @throws Exception Allows implementing methods to throw exceptions without strict try/catch blocks.
         */
        T execute(RedisCommands<K, V> sync) throws Exception;
    }
}