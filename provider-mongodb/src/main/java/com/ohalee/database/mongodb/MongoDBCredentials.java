package com.ohalee.database.mongodb;

import com.ohalee.database.api.DatabaseCredentials;

/**
 * Credentials for connecting to a MongoDB database.
 *
 * @param host                 The hostname of the MongoDB server.
 * @param port                 The port of the MongoDB server.
 * @param username             The username for authentication (null for unauthenticated).
 * @param password             The password for authentication (null for unauthenticated).
 * @param databaseName         The name of the database to connect to.
 * @param authenticationDatabase The database used for authentication (typically "admin").
 * @param connectionString     An optional full connection string that overrides all other fields if set.
 */
public record MongoDBCredentials(
        String host,
        int port,
        String username,
        String password,
        String databaseName,
        String authenticationDatabase,
        String connectionString
) implements DatabaseCredentials {

    /**
     * Creates credentials for a local unauthenticated MongoDB instance.
     *
     * @param databaseName The database name.
     * @return The MongoDB credentials.
     */
    public static MongoDBCredentials from(String databaseName) {
        return from("127.0.0.1", 27017, null, null, databaseName);
    }

    /**
     * Creates credentials with the default port (27017) and no authentication.
     *
     * @param host         The hostname.
     * @param databaseName The database name.
     * @return The MongoDB credentials.
     */
    public static MongoDBCredentials from(String host, String databaseName) {
        return from(host, 27017, null, null, databaseName);
    }

    /**
     * Creates credentials with the default port (27017) and authentication.
     *
     * @param host         The hostname.
     * @param username     The username.
     * @param password     The password.
     * @param databaseName The database name.
     * @return The MongoDB credentials.
     */
    public static MongoDBCredentials from(String host, String username, String password, String databaseName) {
        return from(host, 27017, username, password, databaseName);
    }

    /**
     * Creates credentials with explicit host, port, and optional authentication.
     *
     * @param host         The hostname.
     * @param port         The port.
     * @param username     The username (null for unauthenticated).
     * @param password     The password (null for unauthenticated).
     * @param databaseName The database name.
     * @return The MongoDB credentials.
     */
    public static MongoDBCredentials from(String host, int port, String username, String password, String databaseName) {
        return from(host, port, username, password, databaseName, "admin");
    }

    /**
     * Creates credentials with a custom authentication database.
     *
     * @param host                   The hostname.
     * @param port                   The port.
     * @param username               The username.
     * @param password               The password.
     * @param databaseName           The database name.
     * @param authenticationDatabase The authentication database.
     * @return The MongoDB credentials.
     */
    public static MongoDBCredentials from(String host, int port, String username, String password, String databaseName, String authenticationDatabase) {
        return new MongoDBCredentials(host, port, username, password, databaseName, authenticationDatabase, null);
    }

    /**
     * Creates credentials from a full MongoDB connection string.
     *
     * @param connectionString The MongoDB connection string (e.g. "mongodb://user:pass@host:27017/db").
     * @param databaseName     The database name to use.
     * @return The MongoDB credentials.
     */
    public static MongoDBCredentials fromConnectionString(String connectionString, String databaseName) {
        return new MongoDBCredentials(null, 0, null, null, databaseName, null, connectionString);
    }
}
