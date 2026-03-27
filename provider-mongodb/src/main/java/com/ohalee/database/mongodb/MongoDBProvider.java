package com.ohalee.database.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.ohalee.database.api.DatabaseProvider;

import java.util.List;

/**
 * Implementation of {@link DatabaseProvider} for MongoDB using the MongoDB Java Driver.
 * <p>
 * Returns a {@link MongoDatabase} instance for the configured database.
 * The underlying {@link MongoClient} manages its own connection pool automatically.
 */
public class MongoDBProvider implements DatabaseProvider<MongoDatabase> {

    private final MongoDBCredentials credentials;
    private MongoClient mongoClient;

    /**
     * Creates a new MongoDBProvider with the given credentials.
     *
     * @param credentials The MongoDB credentials.
     */
    public MongoDBProvider(MongoDBCredentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public void connect() {
        if (this.mongoClient != null) return;

        if (this.credentials.connectionString() != null) {
            this.mongoClient = MongoClients.create(new ConnectionString(this.credentials.connectionString()));
            return;
        }

        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                .applyToClusterSettings(builder -> builder.hosts(
                        List.of(new ServerAddress(this.credentials.host(), this.credentials.port()))
                ));

        if (this.credentials.username() != null && this.credentials.password() != null) {
            MongoCredential credential = MongoCredential.createCredential(
                    this.credentials.username(),
                    this.credentials.authenticationDatabase(),
                    this.credentials.password().toCharArray()
            );
            settingsBuilder.credential(credential);
        }

        this.mongoClient = MongoClients.create(settingsBuilder.build());
    }

    @Override
    public void disconnect() {
        if (this.mongoClient != null) {
            this.mongoClient.close();
            this.mongoClient = null;
        }
    }

    @Override
    public MongoDatabase getConnection() {
        return this.mongoClient.getDatabase(this.credentials.databaseName());
    }

    /**
     * Returns the underlying {@link MongoClient} for advanced usage.
     *
     * @return The MongoClient instance.
     */
    public MongoClient getClient() {
        return this.mongoClient;
    }
}
