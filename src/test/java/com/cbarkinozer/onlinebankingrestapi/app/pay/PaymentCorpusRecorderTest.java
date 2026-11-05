package com.cbarkinozer.onlinebankingrestapi.app.pay;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "payments.record", matches = "true")
class PaymentCorpusRecorderTest {
    @Test
    void record() throws Exception {
        Path root = Path.of("payloads/recorded");
        Files.createDirectories(root);
        PaymentRenderer renderer = new PaymentRenderer();
        Random random = new Random(PaymentCorpus.SEED);
        List<Map<String, Object>> entries = new ArrayList<>();
        for (int i = 0; i < 240; i++) {
            PaymentInstruction input = PaymentCorpus.instruction(i, random);
            String id = input.getEndToEndId();
            Path directory = root.resolve(id);
            Files.createDirectories(directory);
            PaymentCorpus.MAPPER.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("input.json").toFile(), input);
            PaymentPayloads output = renderer.render(input);
            Files.writeString(directory.resolve("mt103.txt"), output.getMt103());
            Files.writeString(directory.resolve("pacs008.xml"), output.getPacs008());
            Files.writeString(directory.resolve("event.json"), output.getEvent());
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", id);
            entry.put("debtorTownPresent", true);
            entry.put("creditorTownPresent", i % 40 != 0);
            entries.add(entry);
        }
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("seed", PaymentCorpus.SEED);
        manifest.put("count", entries.size());
        manifest.put("noTownCount", 6);
        manifest.put("recordCommand", "./mvnw -B -Dtest=PaymentCorpusRecorderTest -Dpayments.record=true test");
        manifest.put("payments", entries);
        PaymentCorpus.MAPPER.writerWithDefaultPrettyPrinter().writeValue(root.resolve("manifest.json").toFile(), manifest);
    }
}
