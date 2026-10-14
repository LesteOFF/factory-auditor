package dev.nostalgiacraft.auditor.goal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalEntryTest {
    private static final Path GOALS = Path.of("src/main/resources/data/monitoringcenter_auditor/audit_goals");
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Test
    void readsAnItemGoal() {
        GoalEntry entry = GoalEntry.parse(json("{\"item\":\"minecraft:iron_ingot\",\"base\":60}"));
        assertEquals("minecraft:iron_ingot", entry.item().orElseThrow());
        assertTrue(entry.tag().isEmpty());
        assertEquals(60, entry.base(), 1e-9);
    }

    @Test
    void rejectsANonPositiveBase() {
        assertThrows(IllegalArgumentException.class, () -> GoalEntry.parse(json("{\"base\":0}")));
    }

    @Test
    void rejectsItemAndTagTogether() {
        assertThrows(IllegalArgumentException.class, () -> GoalEntry.parse(json("{\"item\":\"a:b\",\"tag\":\"c:d\",\"base\":1}")));
    }

    @Test
    void everyShippedGoalParsesWithWellFormedIds() throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(GOALS)) {
            files = walk.filter(path -> path.toString().endsWith(".json")).toList();
        }
        assertFalse(files.isEmpty());
        boolean fallback = false;
        for (Path file : files) {
            GoalEntry entry = GoalEntry.parse(json(Files.readString(file, StandardCharsets.UTF_8)));
            entry.item().ifPresent(id -> assertTrue(ID.matcher(id).matches(), file + ": " + id));
            entry.tag().ifPresent(id -> assertTrue(ID.matcher(id).matches(), file + ": " + id));
            fallback |= entry.isFallback();
        }
        assertTrue(fallback, "a goal without item and tag must exist as the default");
    }
}
