package com.ohalee.database.mariadb;

import com.ohalee.database.api.DatabaseCredentials;

/**
 * Credentials for connecting to a MariaDB database.
 *
 * @param host            The hostname of the database server.
 * @param port            The port of the database server.
 * @param username        The username for authentication.
 * @param password        The password for authentication.
 * @param databaseName    The name of the database.
 * @param minimumPoolSize The minimum number of idle connections in the pool.
 * @param maximumPoolSize The maximum number of connections in the pool.
 */
public record MariaDBCredentials(
        String host,
        int port,
        String username,
        String password,
        String databaseName,
        String poolName,
        int minimumPoolSize,
        int maximumPoolSize
) implements DatabaseCredentials {

    /**
     * Creates credentials with the default host (127.0.0.1).
     *
     * @param username     The username.
     * @param password     The password.
     * @param databaseName The database name.
     * @return The MariaDB credentials.
     */
    public static MariaDBCredentials from(String username, String password, String databaseName) {
        return from("127.0.0.1", username, password, databaseName);
    }

    /**
     * Creates credentials with the default port (3306) and pool settings (min 10, max 10).
     *
     * @param host         The hostname.
     * @param username     The username.
     * @param password     The password.
     * @param databaseName The database name.
     * @return The MariaDB credentials.
     */
    public static MariaDBCredentials from(String host, String username, String password, String databaseName) {
        return from(host, 3306, username, password, databaseName, 10, 10);
    }

    /**
     * Creates credentials with default pool settings (min 10, max 10).
     *
     * @param host         The hostname.
     * @param port         The port.
     * @param username     The username.
     * @param password     The password.
     * @param databaseName The database name.
     * @return The MariaDB credentials.
     */
    public static MariaDBCredentials from(String host, int port, String username, String password, String databaseName) {
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
     * @return The MariaDB credentials.
     */
    public static MariaDBCredentials from(String host, int port, String username, String password, String databaseName, int minimumPoolSize, int maximumPoolSize) {
        return from(host, port, username, password, databaseName, null, minimumPoolSize, maximumPoolSize);
    }

    /**
     * Creates credentials with specified pool settings.
     *
     * @param host            The hostname.
     * @param port            The port.
     * @param username        The username.
     * @param password        The password.
     * @param databaseName    The database name.
     * @param poolName        The pool name.
     * @param minimumPoolSize The minimum pool size.
     * @param maximumPoolSize The maximum pool size.
     * @return The MariaDB credentials.
     */
    public static MariaDBCredentials from(String host, int port, String username, String password, String databaseName, String poolName, int minimumPoolSize, int maximumPoolSize) {
        return new MariaDBCredentials(host, port, username, password, databaseName, poolName, minimumPoolSize, maximumPoolSize);
    }
}
