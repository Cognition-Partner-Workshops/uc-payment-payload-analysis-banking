package com.cbarkinozer.onlinebankingrestapi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerfileTest {

    @Test
    void dockerfileUsesNonRootFinalStageAndCanonicalEnvSyntax() throws IOException {
        List<String> lines = Files.readAllLines(Path.of("Dockerfile"));
        int lastFrom = -1;
        boolean appHomeFound = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.toUpperCase().startsWith("FROM ")) {
                lastFrom = i;
            }
            if (line.toUpperCase().startsWith("ENV ")) {
                String assignment = line.substring("ENV ".length()).trim();
                assertCanonicalEnvInstruction(assignment);
                if (assignment.startsWith("APP_HOME=")) {
                    assertEquals("/usr/app", assignment.substring("APP_HOME=".length()));
                    appHomeFound = true;
                }
            }
        }

        assertTrue(lastFrom >= 0, "Dockerfile must contain a build stage");
        assertTrue(appHomeFound, "Dockerfile must define APP_HOME=/usr/app");

        int userLine = -1;
        int commandLine = -1;
        String user = "";
        for (int i = lastFrom + 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.toUpperCase().startsWith("USER ")) {
                userLine = i;
                user = line.substring("USER ".length()).trim();
            }
            if (line.toUpperCase().startsWith("ENTRYPOINT ") || line.toUpperCase().startsWith("CMD ")) {
                commandLine = i;
                break;
            }
        }

        assertTrue(userLine >= 0, "Final build stage must declare a USER");
        assertTrue(commandLine >= 0, "Final build stage must declare ENTRYPOINT or CMD");
        assertTrue(userLine < commandLine, "USER must appear before ENTRYPOINT or CMD");
        assertFalse(user.equalsIgnoreCase("root") || user.equals("0"),
                "Final build stage must use a non-root user");
    }

    private void assertCanonicalEnvInstruction(String assignment) {
        assertFalse(assignment.matches(".*\\s+=.*"), "ENV must not have whitespace before =");
        assertFalse(assignment.matches(".*=\\s+.*"), "ENV must not have whitespace after =");
        assertTrue(assignment.matches("[A-Za-z_][A-Za-z0-9_]*=.*"),
                "ENV must use KEY=value form");
    }
}
