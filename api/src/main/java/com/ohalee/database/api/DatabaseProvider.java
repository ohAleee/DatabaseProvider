package com.ohalee.database.api;

/**
 * Interface defining the lifecycle and contract for a database provider.
 *
 * @param <T> The type of the underlying connection or client.
 */
public interface DatabaseProvider<T> {

    /**
     * Initializes the connection to the database.
     * Implementations should ensure this method is idempotent.
     */
    void connect();

    /**
     * Closes the connection to the database.
     * Should handle closing of pools and resources.
     */
    void disconnect();

    /**
     * Gets the underlying connection object.
     *
     * @return the connection object.
     * @throws Exception if an error occurs
     */
    T getConnection() throws Exception;

}
