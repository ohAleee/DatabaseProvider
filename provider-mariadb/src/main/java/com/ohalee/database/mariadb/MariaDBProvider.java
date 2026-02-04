package com.ohalee.database.mariadb;

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
 * Implementation of {@link DatabaseProvider} for MariaDB using HikariCP.
 */
public class MariaDBProvider implements DatabaseProvider<Connection> {

    private final MariaDBCredentials credentials;
    private HikariDataSource dataSource;

    /**
     * Creates a new MariaDBProvider with the given credentials.
     *
     * @param credentials The MariaDB credentials.
     */
    public MariaDBProvider(MariaDBCredentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public void connect() {
        if (this.dataSource != null) return;

        Properties properties = new Properties();
        properties.setProperty("driverClassName", "org.mariadb.jdbc.Driver");
        properties.setProperty("jdbcUrl", String.format("jdbc:mariadb://%s:%s/%s", this.credentials.host(), this.credentials.port(), this.credentials.databaseName()));
        properties.setProperty("dataSource.serverName", this.credentials.host());
        properties.setProperty("dataSource.user", this.credentials.username());
        properties.setProperty("dataSource.password", this.credentials.password());
        properties.setProperty("dataSource.databaseName", this.credentials.databaseName());
        properties.setProperty("dataSource.portNumber", String.valueOf(this.credentials.port()));

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

    /**
     * Creates a HikariConfig based on the provided properties and credentials.
     *
     * @param properties The base properties.
     * @return The configured HikariConfig.
     */
    protected HikariConfig getHikariConfig(Properties properties) {
        HikariConfig config = new HikariConfig(properties);
        config.setPoolName(this.credentials.poolName());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("allowMultiQueries", true);
        config.addDataSourceProperty("useSSL", false);
        config.addDataSourceProperty("useServerPrepStmts", true);
        config.addDataSourceProperty("cacheResultSetMetadata", true);
        config.addDataSourceProperty("rewriteBatchedStatements", true);
        config.addDataSourceProperty("verifyServerCertificate", false);
        config.addDataSourceProperty("cacheServerConfiguration", true);
        config.setMinimumIdle(this.credentials.minimumPoolSize());
        config.setMaximumPoolSize(this.credentials.maximumPoolSize());
        config.setConnectionTimeout(160000);
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
