package dev.nostalgiacraft.auditor.goal;

import com.google.gson.JsonObject;

import java.util.Optional;

public record GoalEntry(Optional<String> item, Optional<String> tag, double base) {
    public static GoalEntry parse(JsonObject json) {
        Optional<String> item = text(json, "item");
        Optional<String> tag = text(json, "tag");
        if (item.isPresent() && tag.isPresent()) {
            throw new IllegalArgumentException("a goal names either an item or a tag, not both");
        }
        double base = json.has("base") ? json.get("base").getAsDouble() : 0;
        if (!(base > 0)) {
            throw new IllegalArgumentException("base must be a positive number of items per minute");
        }
        return new GoalEntry(item, tag, base);
    }

    private static Optional<String> text(JsonObject json, String key) {
        return json.has(key) ? Optional.of(json.get(key).getAsString()) : Optional.empty();
    }

    public boolean isFallback() {
        return item.isEmpty() && tag.isEmpty();
    }
}
