package com.ohalee.database.h2;

import com.ohalee.database.api.DatabaseCredentials;

/**
 * Credentials for connecting to an H2 database.
 *
 * @param mode            The H2 storage mode (MEMORY, FILE, or SERVER).
 * @param databaseName    The name (or path) of the database.
 * @param host            The host for SERVER mode (ignored for MEMORY and FILE).
 * @param port            The port for SERVER mode (ignored for MEMORY and FILE).
 * @param username        The username for authentication.
 * @param password        The password for authentication.
 * @param poolName        The HikariCP connection pool name.
 * @param minimumPoolSize The minimum number of idle connections in the pool.
 * @param maximumPoolSize The maximum number of connections in the pool.
 */
public record H2Credentials(
        H2Mode mode,
        String databaseName,
        String host,
        int port,
        String username,
        String password,
        String poolName,
        int minimumPoolSize,
        int maximumPoolSize
) implements DatabaseCredentials {

    /**
     * Creates credentials for an in-memory H2 database with default pool settings.
     *
     * @param databaseName The in-memory database name.
     * @return The H2 credentials.
     */
    public static H2Credentials inMemory(String databaseName) {
        return new H2Credentials(H2Mode.MEMORY, databaseName, null, 0, "sa", "", null, 5, 10);
    }

    /**
     * Creates credentials for a file-based H2 database with default pool settings.
     *
     * @param databasePath The file path for the database (e.g. "./data/mydb").
     * @return The H2 credentials.
     */
    public static H2Credentials file(String databasePath) {
        return new H2Credentials(H2Mode.FILE, databasePath, null, 0, "sa", "", null, 5, 10);
    }

    /**
     * Creates credentials for a file-based H2 database with authentication.
     *
     * @param databasePath The file path for the database.
     * @param username     The username.
     * @param password     The password.
     * @return The H2 credentials.
     */
    public static H2Credentials file(String databasePath, String username, String password) {
        return new H2Credentials(H2Mode.FILE, databasePath, null, 0, username, password, null, 5, 10);
    }

    /**
     * Creates credentials for a server-mode H2 database with default port (9092).
     *
     * @param host         The server host.
     * @param databaseName The database name on the server.
     * @param username     The username.
     * @param password     The password.
     * @return The H2 credentials.
     */
    public static H2Credentials server(String host, String databaseName, String username, String password) {
        return server(host, 9092, databaseName, username, password);
    }

    /**
     * Creates credentials for a server-mode H2 database.
     *
     * @param host         The server host.
     * @param port         The server port.
     * @param databaseName The database name on the server.
     * @param username     The username.
     * @param password     The password.
     * @return The H2 credentials.
     */
    public static H2Credentials server(String host, int port, String databaseName, String username, String password) {
        return new H2Credentials(H2Mode.SERVER, databaseName, host, port, username, password, null, 5, 10);
    }
}
