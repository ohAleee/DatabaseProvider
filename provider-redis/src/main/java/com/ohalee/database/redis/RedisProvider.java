package com.ohalee.database.redis;

import com.ohalee.database.api.DatabaseProvider;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.DefaultEventPublisherOptions;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.Delay;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link DatabaseProvider} for Redis using the Lettuce
 * client.
 * <p>
 * This provider manages a {@link RedisClient} and a connection pool.
 * It also supports a separate Pub/Sub connection.
 */
public class RedisProvider implements DatabaseProvider<StatefulRedisConnection<String, String>> {

    private final RedisCredentials credentials;
    private RedisURI redisURI;
    private ClientResources clientResources;
    private ClientOptions clientOptions;
    private RedisClient client;
    private ObjectPool<StatefulRedisConnection<String, String>> pool;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    /**
     * Creates a new RedisProvider with the given credentials.
     *
     * @param credentials The Redis credentials.
     */
    public RedisProvider(RedisCredentials credentials) {
        this.credentials = credentials;
    }

    /**
     * Sets a custom {@link RedisURI} to be used by the RedisClient.
     * Must be called before {@link #connect()}.
     *
     * @param redisURI The RedisURI to use.
     * @return The RedisURI set.
     */
    public RedisURI withRedisURI(RedisURI redisURI) {
        this.redisURI = redisURI;
        return this.redisURI;
    }

    /**
     * Sets the {@link ClientResources} to be used by the RedisClient.
     * Must be called before {@link #connect()}.
     *
     * @param clientResources The client resources to use.
     */
    public RedisProvider withClientResources(ClientResources clientResources) {
        this.clientResources = clientResources;
        return this;
    }

    /**
     * Sets the {@link ClientOptions} to be used by the RedisClient.
     * Must be called before {@link #connect()}.
     *
     * @param clientOptions The client options to use.
     */
    public RedisProvider withClientOptions(ClientOptions clientOptions) {
        this.clientOptions = clientOptions;
        return this;
    }

    @Override
    public void connect() {
        if (this.client != null) return;

        if (this.redisURI == null) {
            RedisURI.Builder builder = RedisURI.builder()
                    .withHost(this.credentials.host())
                    .withPort(this.credentials.port())
                    .withDatabase(this.credentials.database());

            if (this.credentials.clientName() != null && !this.credentials.clientName().isBlank()) {
                builder.withClientName(this.credentials.clientName());
            }

            if (this.credentials.username() != null && !this.credentials.username().isBlank()) {
                builder.withAuthentication(this.credentials.username(), this.credentials.password());
            } else if (this.credentials.password() != null && !this.credentials.password().isBlank()) {
                builder.withPassword(this.credentials.password().toCharArray());
            }

            this.redisURI = builder.build();
        }

        if (this.clientResources == null) {
            this.clientResources = DefaultClientResources.builder()
                    .ioThreadPoolSize(64)
                    .computationThreadPoolSize(32)
                    .commandLatencyPublisherOptions(DefaultEventPublisherOptions.create())
                    .reconnectDelay(Delay.decorrelatedJitter(100, 1000, 2, TimeUnit.MILLISECONDS))
                    .build();
        }

        this.client = RedisClient.create(this.clientResources, redisURI);

        if (this.clientOptions == null) {
            this.clientOptions = ClientOptions.builder()
                    .timeoutOptions(TimeoutOptions.builder()
                            .timeoutCommands(true)
                            .fixedTimeout(Duration.ofSeconds(5))
                            .build())
                    .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                    .autoReconnect(true)
                    .build();
        }
        this.client.setOptions(this.clientOptions);

        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(this.credentials.maximumPoolSize());
        poolConfig.setMaxIdle(this.credentials.maximumPoolSize());
        poolConfig.setMinIdle(this.credentials.minimumPoolSize());

        this.pool = ConnectionPoolSupport.createGenericObjectPool(() -> this.client.connect(), poolConfig);
        this.pubSubConnection = this.client.connectPubSub(StringCodec.UTF8, redisURI);
    }

    @Override
    public void disconnect() {
        if (this.pubSubConnection != null) {
            this.pubSubConnection.close();
        }
        if (this.pool != null) {
            this.pool.close();
        }
        if (this.client != null) {
            this.client.shutdown();
        }
    }

    @Override
    public StatefulRedisConnection<String, String> getConnection() {
        if (this.pool == null) {
            throw new IllegalStateException("Connection pool is not initialized");
        }
        try {
            return this.pool.borrowObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to borrow connection from pool", e);
        }
    }

    /**
     * Gets the dedicated Pub/Sub connection.
     *
     * @return The Pub/Sub connection.
     */
    public StatefulRedisPubSubConnection<String, String> pubSubConnection() {
        return this.pubSubConnection;
    }
}
