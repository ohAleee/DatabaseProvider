package com.ohalee.database.redis.builtin;

import com.ohalee.database.redis.LettuceOperations;
import com.ohalee.database.redis.RedisProvider;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * A Redis connection implementation for String key-value pairs.
 *
 * @see com.ohalee.database.redis.LettuceOperations
 * @see <a href="https://lettuce.io/core/release/api/">Lettuce API Documentation</a>
 */
public class StringRedisOperations extends LettuceOperations<String, String> {

    private final RedisProvider provider;

    /**
     * Constructs a StringRedisConnection with the given RedisProvider.
     *
     * @param provider the RedisProvider to obtain connections from.
     */
    public StringRedisOperations(RedisProvider provider) {
        this.provider = provider;
    }

    /**
     * Provides a dedicated Redis connection for String key-value pairs.
     *
     * @return a StatefulRedisConnection instance for String keys and values.
     */
    @Override
    protected StatefulRedisConnection<String, String> connection() {
        return this.provider.getConnection();
    }

}