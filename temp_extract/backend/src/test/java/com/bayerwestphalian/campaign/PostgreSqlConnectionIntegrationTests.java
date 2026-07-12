package com.bayerwestphalian.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

class PostgreSqlConnectionIntegrationTests {

    private static final String DB_URL =
            System.getProperty("bwc.test.db.url", "jdbc:postgresql://localhost:5432/bwc_campaign");
    private static final String DB_USERNAME = System.getProperty("bwc.test.db.username", "bwc_app");
    private static final String DB_PASSWORD = System.getProperty("bwc.test.db.password", "bwc_app");

    @Test
    void connectsToLocalDockerComposePostgreSqlAndRunsValidationQuery() throws Exception {
        assumeTrue(isPortOpen("localhost", 5432), "Local PostgreSQL is not running on port 5432");

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "select current_database() as database, current_user as username, 1 as check_value")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("database")).isEqualTo("bwc_campaign");
            assertThat(resultSet.getString("username")).isEqualTo("bwc_app");
            assertThat(resultSet.getInt("check_value")).isEqualTo(1);
        }
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1_000);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
