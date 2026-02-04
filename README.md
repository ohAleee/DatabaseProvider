# DatabaseProvider
[![Maven Central](https://img.shields.io/maven-central/v/com.ohalee.database/api.svg)](https://central.sonatype.com/artifact/com.ohalee.database/api)
[![MIT License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/ohAleee/DatabaseProvider/blob/master/LICENSE)

A flexible and lightweight Java library providing unified abstractions for database connections with built-in support
for MariaDB and Redis.

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
  - [MariaDB Setup](#mariadb-setup)
  - [Redis Setup](#redis-setup)
- [Advanced Usage](#advanced-usage)
  - [MariaDB Schema Loading](#mariadb-schema-loading)
  - [Redis Custom Configuration](#redis-custom-configuration)
  - [Redis Pub/Sub](#redis-pubsub)
- [API Reference](#api-reference)

## Features

- 🚀 **Simple API**: Clean, interface-based design for easy implementation and testing
- ⚡ **Connection Pooling**: Built-in connection pooling for MariaDB (HikariCP) and Redis (Lettuce)
- 🧩 **Multiple Database Support**: MariaDB and Redis providers out of the box
- 📚 **Schema Management**: Built-in SQL schema loading for MariaDB
- 📡 **Pub/Sub Support**: Dedicated Redis Pub/Sub connection handling
- ✨ **Java 21**: Modern Java with the latest features

## Requirements

- Java 21 or higher

## Installation

DatabaseProvider is available on Maven Central.

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    // For MariaDB
    implementation("com.ohalee.database:provider-mariadb:{version}")

    // For Redis
    implementation("com.ohalee.database:provider-redis:{version}")

    // Or just the API if you want to implement your own provider
    api("com.ohalee.database:api:{version}")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    // For MariaDB
    implementation 'com.ohalee.database:provider-mariadb:{version}'

    // For Redis
    implementation 'com.ohalee.database:provider-redis:{version}'
  
    // Or just the API if you want to implement your own provider
    api 'com.ohalee.database:api:{version}'
}
```

### Maven

```xml
<!-- For MariaDB -->
<dependency>
    <groupId>com.ohalee.database</groupId>
    <artifactId>provider-mariadb</artifactId>
    <version>{version}</version>
</dependency>

<!-- For Redis -->
<dependency>
  <groupId>com.ohalee.database</groupId>
  <artifactId>provider-redis</artifactId>
  <version>{version}</version>
</dependency>

<!-- Or just the API if you want to implement your own provider -->
<dependency>
  <groupId>com.ohalee.database</groupId>
  <artifactId>api</artifactId>
  <version>{version}</version>
</dependency>
```

## Quick Start

### MariaDB Setup

```java
import com.ohalee.database.mariadb.MariaDBCredentials;
import com.ohalee.database.mariadb.MariaDBProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MariaDBExample {
    
    public static void main(String[] args) throws Exception {
        MariaDBCredentials credentials = MariaDBCredentials.from("localhost", "username", "password", "database");

        MariaDBProvider provider = new MariaDBProvider(credentials);
        provider.connect();

        try (Connection connection = provider.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users WHERE id = ?")) {

            stmt.setInt(1, 1);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                System.out.println("User: " + rs.getString("username"));
            }
        }

        provider.disconnect();
    }
}
```

### Redis Setup

```java
import com.ohalee.database.redis.RedisCredentials;
import com.ohalee.database.redis.RedisProvider;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

public class RedisExample {
    
    public static void main(String[] args) {
        RedisCredentials credentials = RedisCredentialsl.from("localhost", 6379);

        RedisProvider provider = new RedisProvider(credentials);
        provider.connect();

        // See also LettuceOperations class for advanced usage
        StatefulRedisConnection<String, String> connection = provider.getConnection();
        RedisCommands<String, String> commands = connection.sync();

        commands.set("user:1:name", "John Doe");
        String name = commands.get("user:1:name");
        System.out.println("Name: " + name);

        // Return connection to pool (if using try-with-resources)
        connection.close();

        provider.disconnect();
    }
}
```

## Advanced Usage

### MariaDB Schema Loading

DatabaseProvider includes a convenient schema loading feature for MariaDB:

```java
// Load schema from a file
provider.loadSchema(new File("schema.sql"));

// Or load from classpath resources
try (InputStream stream = getClass().getResourceAsStream("/schema.sql")) {
    provider.loadSchema(stream);
}
```

**Example schema.sql:**

```sql
CREATE TABLE IF NOT EXISTS users
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL,
    email      VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sessions
(
    id         VARCHAR(36) PRIMARY KEY,
    user_id    INT       NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id)
);
```

The schema loader supports:

- Multiple statements in a single file
- SQL comments (`--` and `#`)
- Custom delimiters (`DELIMITER` command)
- Stored procedures and triggers

### Redis Custom Configuration

You can customize Redis client options before connecting:

```java
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

import java.time.Duration;

...

RedisProvider provider = new RedisProvider(credentials);

// Custom Redis URI with SSL
RedisURI redisURI = RedisURI.builder()
        .withHost("redis.example.com")
        .withPort(6380)
        .withSsl(true)
        .withVerifyPeer(true)
        .withDatabase(0)
        .withAuthentication("username", "password")
        .build();
provider.withRedisURI(redisURI);

// Custom client resources for thread pool tuning
ClientResources resources = DefaultClientResources.builder()
        .ioThreadPoolSize(32)
        .computationThreadPoolSize(16)
        .build();
provider.withClientResources(resources);

// Custom client options
ClientOptions options = ClientOptions.builder()
        .timeoutOptions(TimeoutOptions.builder()
                .fixedTimeout(Duration.ofSeconds(10))
                .build())
        .autoReconnect(true)
        .build();
provider.withClientOptions(options);

// Now connect with custom configuration
provider.connect();
```

### Redis Pub/Sub

DatabaseProvider provides a dedicated Pub/Sub connection for Redis:

```java
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

...

RedisProvider provider = new RedisProvider(credentials);
provider.connect();

// Get the Pub/Sub connection
StatefulRedisPubSubConnection<String, String> pubSub = provider.pubSubConnection();

// Add a listener
pubSub.addListener(new RedisPubSubListener<>() {
    @Override
    public void message (String channel, String message){
        System.out.println("Received on " + channel + ": " + message);
    }

    @Override
    public void message (String pattern, String channel, String message){
        // Pattern subscription message
    }

    @Override
    public void subscribed (String channel,long count){
        System.out.println("Subscribed to " + channel);
    }

    @Override
    public void unsubscribed (String channel,long count){
        System.out.println("Unsubscribed from " + channel);
    }

    @Override
    public void psubscribed (String pattern,long count){
        // Pattern subscription
    }

    @Override
    public void punsubscribed (String pattern,long count){
        // Pattern unsubscription
    }
});

// Subscribe to channels
pubSub.sync().subscribe("notifications","events");

// Publish messages from another connection
StatefulRedisConnection<String, String> connection = provider.getConnection();
connection.sync().publish("notifications","Hello, World!");
```

## API Reference

### Core Interfaces

#### DatabaseProvider<T>

The main interface for all database providers:

```java
public interface DatabaseProvider<T> {
    void connect();                      // Initialize connection

    void disconnect();                   // Close connection

    T getConnection() throws Exception;  // Get underlying connection
}
```

#### DatabaseCredentials

Marker interface for credentials. Implementations:

- `MariaDBCredentials(host, port, databaseName, username, password, minPoolSize, maxPoolSize)`
- `RedisCredentials(host, port, database, username, password, minPoolSize, maxPoolSize)`

### MariaDBProvider

```java
// Constructor
MariaDBProvider(MariaDBCredentials credentials)

// Methods
void connect()

void disconnect()

Connection getConnection() throws SQLException

void loadSchema(File file) throws SQLException, IOException

void loadSchema(InputStream stream) throws SQLException, IOException

protected HikariConfig getHikariConfig(Properties properties)
```

### RedisProvider

```java
// Constructor
RedisProvider(RedisCredentials credentials)

// Methods
void connect()

void disconnect()

StatefulRedisConnection<String, String> getConnection()

RedisURI withRedisURI(RedisURI redisURI)

RedisProvider withClientResources(ClientResources clientResources)

RedisProvider withClientOptions(ClientOptions clientOptions)

StatefulRedisPubSubConnection<String, String> pubSubConnection()
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.