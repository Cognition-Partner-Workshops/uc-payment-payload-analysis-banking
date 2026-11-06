package com.cbarkinozer.onlinebankingrestapi.app.sec.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommittedSecretsTest {

    private static final Path MAIN_PROPERTIES = Paths.get("src/main/resources/application.properties");
    private static final Path TEST_PROPERTIES = Paths.get("src/test/resources/application.properties");

    @Test
    void mainJwtSigningKeyComesFromEnvironmentWithoutDefault() throws IOException {
        assertEquals("${JWT_SIGNING_KEY}",
                load(MAIN_PROPERTIES).getProperty("onlinebankingrestapi.jwt.security.app.key"));
    }

    @Test
    void mainDatasourcePasswordComesFromEnvironmentWithoutDefault() throws IOException {
        assertEquals("${DB_PASSWORD}",
                load(MAIN_PROPERTIES).getProperty("spring.datasource.password"));
    }

    @Test
    void testJwtSigningKeyIsNotACommittedLiteral() throws IOException {
        String key = load(TEST_PROPERTIES).getProperty("onlinebankingrestapi.jwt.security.app.key");
        assertTrue(key.startsWith("${JWT_SIGNING_KEY:${random."),
                "test signing key must come from JWT_SIGNING_KEY or a per-run random value");
    }

    private static Properties load(Path path) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        }
        return properties;
    }
}
