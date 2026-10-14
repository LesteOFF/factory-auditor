package dev.nostalgiacraft.auditor.award;

import java.util.Comparator;
import java.util.List;

public record Milestones(List<Milestone> ordered) {
    public static final Milestones NONE = new Milestones(List.of());

    public static Milestones of(List<Milestone> milestones) {
        return new Milestones(milestones.stream().sorted(Comparator.comparingInt(Milestone::seals)).toList());
    }

    public List<Milestone> due(int seals, int deliveredUpTo) {
        return ordered.stream().filter(milestone -> milestone.seals() > deliveredUpTo && milestone.seals() <= seals).toList();
    }
}
