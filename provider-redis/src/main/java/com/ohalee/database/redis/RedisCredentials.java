package com.ohalee.database.redis;

import com.ohalee.database.api.DatabaseCredentials;

/**
 * Credentials for connecting to a Redis database.
 *
 * @param host            The hostname of the Redis server.
 * @param port            The port of the Redis server.
 * @param username        The username for authentication (optional).
 * @param password        The password for authentication (optional).
 * @param database        The database index to use.
 * @param minimumPoolSize The minimum number of idle connections in the pool.
 * @param maximumPoolSize The maximum number of connections in the pool.
 */
public record RedisCredentials(
        String host,
        int port,
        String username,
        String password,
        int database,
        String clientName,
        int minimumPoolSize,
        int maximumPoolSize
) implements DatabaseCredentials {

    /**
     * Creates credentials with the default port (6379), default pool settings (min 5,
     * max 10), and database 0.
     *
     * @param host The hostname.
     * @return The Redis credentials.
     */
    public static RedisCredentials from(String host, int port) {
        return from(host, port, null);
    }

    /**
     * Creates credentials with the default port (6379), default pool settings (min 5,
     * max 10), and database 0.
     *
     * @param host     The hostname.
     * @param password The password.
     * @return The Redis credentials.
     */
    public static RedisCredentials from(String host, String password) {
        return from(host, 6379, password);
    }

    /**
     * Creates credentials with default pool settings (min 5, max 10) and database
     * 0.
     *
     * @param host     The hostname.
     * @param port     The port.
     * @param password The password.
     * @return The Redis credentials.
     */
    public static RedisCredentials from(String host, int port, String password) {
        return from(host, port, null, password, 0);
    }

    /**
     * Creates credentials with default pool settings (min 5, max 10).
     *
     * @param host     The hostname.
     * @param port     The port.
     * @param username The username.
     * @param password The password.
     * @return The Redis credentials.
     */
    public static RedisCredentials from(String host, int port, String username, String password) {
        return from(host, port, username, password, 0);
    }

    /**
     * Creates credentials with default pool settings (min 5, max 10).
     *
     * @param host     The hostname.
     * @param port     The port.
     * @param username The username.
     * @param password The password.
     * @param database The database index.
     * @return The Redis credentials.
     */
    public static RedisCredentials from(String host, int port, String username, String password, int database) {
        return new RedisCredentials(host, port, username, password, database, null, 5, 10);
    }

    public static RedisCredentials from(String host, int port, String username, String password, int database, int minimumPoolSize, int maximumPoolSize) {
        return from(host, port, username, password, database, null, minimumPoolSize, maximumPoolSize);
    }

    /**
     * Creates credentials with specified pool settings.
     *
     * @param host            The hostname.
     * @param port            The port.
     * @param username        The username.
     * @param password        The password.
     * @param database        The database index.
     * @param clientName      The client name.
     * @param minimumPoolSize The minimum pool size.
     * @param maximumPoolSize The maximum pool size.
     * @return The Redis credentials.
     */
    public static RedisCredentials from(String host, int port, String username, String password, int database, String clientName, int minimumPoolSize, int maximumPoolSize) {
        return new RedisCredentials(host, port, username, password, database, clientName, minimumPoolSize, maximumPoolSize);
    }
}
