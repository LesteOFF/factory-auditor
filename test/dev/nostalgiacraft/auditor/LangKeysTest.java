package dev.nostalgiacraft.auditor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LangKeysTest {
    private static final Path LANG = Path.of("src/main/resources/assets/monitoringcenter_auditor/lang");

    private static Set<String> keys(String language) throws IOException {
        String json = Files.readString(LANG.resolve(language + ".json"), StandardCharsets.UTF_8);
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        return object.keySet();
    }

    @Test
    void englishAndPortugueseShareTheSameKeys() throws IOException {
        assertEquals(keys("en_us"), keys("pt_br"));
    }
}
