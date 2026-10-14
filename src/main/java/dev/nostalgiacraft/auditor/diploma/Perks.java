package dev.nostalgiacraft.auditor.diploma;

import java.util.Set;

public record Perks(int minutes, int forgivable, boolean excellent) {
    private static final double EXPRESS_SHARE = 0.6;

    public static Perks of(Set<Diploma> diplomas, int baseMinutes) {
        int minutes = diplomas.contains(Diploma.SENIOR) ? Math.max(1, (int) Math.ceil(baseMinutes * EXPRESS_SHARE)) : baseMinutes;
        return new Perks(minutes, diplomas.contains(Diploma.JUNIOR) ? 1 : 0, diplomas.contains(Diploma.MASTER));
    }
}
