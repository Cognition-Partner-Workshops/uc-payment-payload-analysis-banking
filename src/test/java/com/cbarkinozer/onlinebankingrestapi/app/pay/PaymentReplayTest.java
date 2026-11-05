package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import static org.junit.jupiter.api.Assertions.*;

class PaymentReplayTest {
    @TestFactory
    List<DynamicTest> replay() throws Exception {
        Path root = Path.of("payloads/recorded");
        JsonNode manifest = PaymentCorpus.MAPPER.readTree(root.resolve("manifest.json").toFile());
        assertEquals(240, manifest.get("count").asInt());
        assertEquals(240, manifest.get("payments").size());
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode entry : manifest.get("payments")) {
            String id = entry.get("id").asText();
            tests.add(DynamicTest.dynamicTest(id, () -> {
                Path directory = root.resolve(id);
                PaymentInstruction input = PaymentCorpus.MAPPER.readValue(directory.resolve("input.json").toFile(), PaymentInstruction.class);
                PaymentPayloads output = new PaymentRenderer().render(input);
                assertArrayEquals(Files.readAllBytes(directory.resolve("mt103.txt")), output.getMt103().getBytes(StandardCharsets.UTF_8));
                assertArrayEquals(Files.readAllBytes(directory.resolve("pacs008.xml")), output.getPacs008().getBytes(StandardCharsets.UTF_8));
                assertArrayEquals(Files.readAllBytes(directory.resolve("event.json")), output.getEvent().getBytes(StandardCharsets.UTF_8));
            }));
        }
        return tests;
    }
}
