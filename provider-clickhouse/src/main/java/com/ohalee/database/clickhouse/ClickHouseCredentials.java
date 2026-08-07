package com.ohalee.database.clickhouse;

import com.ohalee.database.api.DatabaseCredentials;

/**
 * Credentials for connecting to a ClickHouse database.
 *
 * @param host            The hostname of the ClickHouse server.
 * @param port            The HTTP port of the ClickHouse server (8123 plain, 8443 with SSL).
 * @param username        The username for authentication.
 * @param password        The password for authentication (may be empty).
 * @param databaseName    The name of the database.
 * @param ssl             Whether the connection should use SSL/TLS.
 * @param poolName        The HikariCP connection pool name.
 * @param minimumPoolSize The minimum number of idle connections in the pool.
 * @param maximumPoolSize The maximum number of connections in the pool.
 */
public record ClickHouseCredentials(
        String host,
        int port,
        String username,
        String password,
        String databaseName,
        boolean ssl,
        String poolName,
        int minimumPoolSize,
        int maximumPoolSize
) implements DatabaseCredentials {

    /**
     * The default ClickHouse HTTP port.
     */
    public static final int DEFAULT_PORT = 8123;

    /**
     * The default ClickHouse HTTPS port, used by ClickHouse Cloud.
     */
    public static final int DEFAULT_SECURE_PORT = 8443;

    /**
     * The default ClickHouse user.
     */
    public static final String DEFAULT_USER = "default";

    /**
     * The default ClickHouse database.
     */
    public static final String DEFAULT_DATABASE = "default";

    /**
     * Creates credentials with the default host (127.0.0.1).
     *
     * @param username     The username.
     * @param password     The password.
     * @param databaseName The database name.
     * @return The ClickHouse credentials.
     */
    public static ClickHouseCredentials from(String username, String password, String databaseName) {
        return from("127.0.0.1", username, password, databaseName);
    }

    /**
     * Creates credentials with the default port (8123) and pool settings (min 10, max 10).
     *
     * @param host         The hostname.
     * @param username     The username.
     * @param password     The password.
     * @param databaseName The database name.
     * @return The ClickHouse credentials.
     */
    public static ClickHouseCredentials from(String host, String username, String password, String databaseName) {
        return from(host, DEFAULT_PORT, username, password, databaseName);
    }

    /**
     * Creates credentials with default pool settings (min 10, max 10).
     *
     * @param host         The hostname.
     * @param port         The port.
     * @param username     The username.
     * @param password     The password.
     * @param databaseName The database name.
     * @return The ClickHouse credentials.
     */
    public static ClickHouseCredentials from(String host, int port, String username, String password, String databaseName) {
        return from(host, port, username, password, databaseName, 10, 10);
    }

    /**
     * Creates credentials with specified pool settings.
     *
     * @param host            The hostname.
     * @param port            The port.
     * @param username        The username.
     * @param password        The password.
     * @param databaseName    The database name.
     * @param minimumPoolSize The minimum pool size.
     * @param maximumPoolSize The maximum pool size.
     * @return The ClickHouse credentials.
     */
    public static ClickHouseCredentials from(String host, int port, String username, String password, String databaseName, int minimumPoolSize, int maximumPoolSize) {
        return from(host, port, username, password, databaseName, false, null, minimumPoolSize, maximumPoolSize);
    }

    /**
     * Creates credentials with specified SSL and pool settings.
     *
     * @param host            The hostname.
     * @param port            The port.
     * @param username        The username.
     * @param password        The password.
     * @param databaseName    The database name.
     * @param ssl             Whether to use SSL/TLS.
     * @param poolName        The pool name.
     * @param minimumPoolSize The minimum pool size.
     * @param maximumPoolSize The maximum pool size.
     * @return The ClickHouse credentials.
     */
    public static ClickHouseCredentials from(String host, int port, String username, String password, String databaseName, boolean ssl, String poolName, int minimumPoolSize, int maximumPoolSize) {
        return new ClickHouseCredentials(host, port, username, password, databaseName, ssl, poolName, minimumPoolSize, maximumPoolSize);
    }

    /**
     * Creates credentials for a ClickHouse Cloud instance, using the secure port (8443),
     * SSL, the {@code default} user and the {@code default} database.
     *
     * @param host     The ClickHouse Cloud hostname.
     * @param password The password of the {@code default} user.
     * @return The ClickHouse credentials.
     */
    public static ClickHouseCredentials cloud(String host, String password) {
        return cloud(host, DEFAULT_USER, password, DEFAULT_DATABASE);
    }

    /**
     * Creates credentials for a ClickHouse Cloud instance, using the secure port (8443) and SSL.
     *
     * @param host         The ClickHouse Cloud hostname.
     * @param username     The username.
     * @param password     The password.
     * @param databaseName The database name.
     * @return The ClickHouse credentials.
     */
    public static ClickHouseCredentials cloud(String host, String username, String password, String databaseName) {
        return from(host, DEFAULT_SECURE_PORT, username, password, databaseName, true, null, 10, 10);
    }
}
