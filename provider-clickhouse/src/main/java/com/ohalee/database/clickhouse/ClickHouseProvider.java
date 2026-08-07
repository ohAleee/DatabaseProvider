package com.ohalee.database.clickhouse;

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
 * Implementation of {@link DatabaseProvider} for ClickHouse using HikariCP.
 * <p>
 * Connections are established over the ClickHouse HTTP interface
 * (URL: {@code jdbc:clickhouse://host:port/database}), optionally secured with SSL
 * for ClickHouse Cloud and other TLS-terminated deployments.
 */
public class ClickHouseProvider implements DatabaseProvider<Connection> {

    private static final String DRIVER_CLASS_NAME = "com.clickhouse.jdbc.ClickHouseDriver";

    private final ClickHouseCredentials credentials;
    private HikariDataSource dataSource;

    /**
     * Creates a new ClickHouseProvider with the given credentials.
     *
     * @param credentials The ClickHouse credentials.
     */
    public ClickHouseProvider(ClickHouseCredentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public void connect() {
        if (this.dataSource != null) return;

        Properties properties = new Properties();
        properties.setProperty("driverClassName", DRIVER_CLASS_NAME);
        properties.setProperty("jdbcUrl", buildJdbcUrl());
        properties.setProperty("username", this.credentials.username() == null ? ClickHouseCredentials.DEFAULT_USER : this.credentials.username());
        properties.setProperty("password", this.credentials.password() == null ? "" : this.credentials.password());

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
        String databaseName = this.credentials.databaseName() == null
                ? ClickHouseCredentials.DEFAULT_DATABASE
                : this.credentials.databaseName();

        return String.format("jdbc:clickhouse://%s:%d/%s", this.credentials.host(), this.credentials.port(), databaseName);
    }

    /**
     * Creates a HikariConfig based on the provided properties and credentials.
     * <p>
     * ClickHouse does not support interactive transactions, so the pool is kept in
     * auto-commit mode and no transaction isolation is requested.
     *
     * @param properties The base properties.
     * @return The configured HikariConfig.
     */
    protected HikariConfig getHikariConfig(Properties properties) {
        HikariConfig config = new HikariConfig(properties);
        if (this.credentials.poolName() != null) {
            config.setPoolName(this.credentials.poolName());
        }
        config.addDataSourceProperty("ssl", String.valueOf(this.credentials.ssl()));
        config.addDataSourceProperty("compress", "true");
        config.addDataSourceProperty("client_name", "DatabaseProvider");
        config.setAutoCommit(true);
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
            String line;

            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();

                if (trimmedLine.isEmpty() || trimmedLine.startsWith("--") || trimmedLine.startsWith("#")) {
                    continue;
                }

                statementBuilder.append(line).append("\n");

                if (trimmedLine.endsWith(";")) {
                    String sql = statementBuilder.toString();
                    sql = sql.substring(0, sql.lastIndexOf(";"));

                    if (!sql.trim().isEmpty()) {
                        statement.execute(sql);
                    }

                    statementBuilder.setLength(0);
                }
            }
        }
    }
}
