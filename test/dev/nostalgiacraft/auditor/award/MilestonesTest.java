package dev.nostalgiacraft.auditor.award;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MilestonesTest {
    private static final Path DIPLOMAS = Path.of("src/main/resources/data/monitoringcenter_auditor/diplomas");
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Milestone FIRST = new Milestone(1, "a:diploma");
    private static final Milestone FIFTH = new Milestone(5, "a:trophy");
    private static final Milestones MILESTONES = Milestones.of(List.of(FIFTH, FIRST));

    @Test
    void theFirstSealEarnsTheFirstPrize() {
        assertEquals(List.of(FIRST), MILESTONES.due(1, 0));
    }

    @Test
    void prizesComeInOrderOfSeals() {
        assertEquals(List.of(FIRST, FIFTH), MILESTONES.due(6, 0));
    }

    @Test
    void deliveredPrizesAreNotRepeated() {
        assertEquals(List.of(FIFTH), MILESTONES.due(6, 1));
        assertEquals(List.of(), MILESTONES.due(6, 5));
    }

    @Test
    void aMilestoneAddedBelowTheLastDeliveredOneIsNotRepeatedNorShiftsTheNext() {
        Milestones changed = Milestones.of(List.of(FIRST, new Milestone(3, "a:new"), FIFTH, new Milestone(9, "a:next")));
        assertEquals(List.of(new Milestone(9, "a:next")), changed.due(9, 5));
    }

    @Test
    void aMilestoneNeedsAPositiveSealCountAndAnItem() {
        assertThrows(IllegalArgumentException.class, () -> Milestone.parse(JsonParser.parseString("{\"seals\":0,\"item\":\"a:b\"}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> Milestone.parse(JsonParser.parseString("{\"seals\":3}").getAsJsonObject()));
    }

    @Test
    void theShippedPrizesAreFiveWithDistinctSealCounts() throws IOException {
        List<Milestone> shipped;
        try (Stream<Path> files = Files.list(DIPLOMAS)) {
            shipped = files.filter(path -> path.toString().endsWith(".json")).map(MilestonesTest::read).toList();
        }
        assertEquals(5, shipped.size());
        assertEquals(5, shipped.stream().mapToInt(Milestone::seals).distinct().count());
        shipped.forEach(milestone -> assertTrue(ID.matcher(milestone.item()).matches(), milestone.item()));
    }

    private static Milestone read(Path path) {
        try {
            return Milestone.parse(JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
