package com.ohalee.database.h2;

import com.ohalee.database.api.DatabaseProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Implementation of {@link DatabaseProvider} for H2 using HikariCP.
 * <p>
 * Supports all three H2 modes:
 * <ul>
 *   <li>{@link H2Mode#MEMORY} - in-memory database (URL: {@code jdbc:h2:mem:name})</li>
 *   <li>{@link H2Mode#FILE} - file-based embedded database (URL: {@code jdbc:h2:./path})</li>
 *   <li>{@link H2Mode#SERVER} - remote server database (URL: {@code jdbc:h2:tcp://host:port/name})</li>
 * </ul>
 */
public class H2Provider implements DatabaseProvider<Connection> {

    private final H2Credentials credentials;
    private HikariDataSource dataSource;

    /**
     * Creates a new H2Provider with the given credentials.
     *
     * @param credentials The H2 credentials.
     */
    public H2Provider(H2Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public void connect() {
        if (this.dataSource != null) return;

        Properties properties = new Properties();
        properties.setProperty("driverClassName", "org.h2.Driver");
        properties.setProperty("jdbcUrl", buildJdbcUrl());
        properties.setProperty("dataSource.user", this.credentials.username());
        properties.setProperty("dataSource.password", this.credentials.password());

        this.dataSource = new HikariDataSource(getHikariConfig(properties));
    }

    @Override
    public void disconnect() {
        if (this.dataSource != null && !this.dataSource.isClosed()) {
            this.dataSource.close();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    private String buildJdbcUrl() {
        return switch (this.credentials.mode()) {
            case MEMORY -> "jdbc:h2:mem:" + this.credentials.databaseName() + ";DB_CLOSE_DELAY=-1";
            case FILE -> {
                String path = this.credentials.databaseName().replace("\\", "/");
                if (!path.startsWith("/") && !path.startsWith("./") && !path.startsWith("~/")) {
                    path = "./" + path;
                }
                yield "jdbc:h2:" + path;
            }
            case SERVER -> String.format("jdbc:h2:tcp://%s:%d/%s",
                    this.credentials.host(), this.credentials.port(), this.credentials.databaseName());
        };
    }

    /**
     * Creates a HikariConfig from the provided properties and credentials.
     *
     * @param properties The base properties.
     * @return The configured HikariConfig.
     */
    protected HikariConfig getHikariConfig(Properties properties) {
        HikariConfig config = new HikariConfig(properties);
        if (this.credentials.poolName() != null) {
            config.setPoolName(this.credentials.poolName());
        }
        config.setMinimumIdle(this.credentials.minimumPoolSize());
        config.setMaximumPoolSize(this.credentials.maximumPoolSize());
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        return config;
    }

    /**
     * Loads and executes a SQL schema file.
     *
     * @param file The SQL file to load.
     * @throws SQLException If a database access error occurs.
     * @throws IOException  If an I/O error occurs reading the file.
     */
    public void loadSchema(File file) throws SQLException, IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            loadSchema(inputStream);
        }
    }

    /**
     * Loads and executes a SQL schema from an InputStream.
     *
     * @param stream The InputStream containing the SQL schema.
     * @throws SQLException If a database access error occurs.
     * @throws IOException  If an I/O error occurs reading the stream.
     */
    public void loadSchema(InputStream stream) throws SQLException, IOException {
        try (Connection connection = this.dataSource.getConnection()) {
            executeScript(connection, stream);
        }
    }

    private void executeScript(Connection connection, InputStream inputStream) throws SQLException, IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             Statement statement = connection.createStatement()) {

            StringBuilder statementBuilder = new StringBuilder();
            String currentDelimiter = ";";
            String line;

            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();

                if (trimmedLine.isEmpty() || trimmedLine.startsWith("--") || trimmedLine.startsWith("#")) {
                    continue;
                }

                if (trimmedLine.toUpperCase().startsWith("DELIMITER")) {
                    currentDelimiter = trimmedLine.substring(9).trim();
                    continue;
                }

                statementBuilder.append(line).append("\n");

                if (trimmedLine.endsWith(currentDelimiter)) {
                    String sql = statementBuilder.toString();
                    sql = sql.substring(0, sql.lastIndexOf(currentDelimiter));

                    if (!sql.trim().isEmpty()) {
                        statement.execute(sql);
                    }

                    statementBuilder.setLength(0);
                }
            }
        }
    }
}
