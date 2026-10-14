package dev.nostalgiacraft.auditor.award;

import com.google.gson.JsonObject;

public record Milestone(int seals, String item) {
    public static Milestone parse(JsonObject json) {
        int seals = json.has("seals") ? json.get("seals").getAsInt() : 0;
        if (seals < 1) {
            throw new IllegalArgumentException("seals must be at least 1");
        }
        if (!json.has("item")) {
            throw new IllegalArgumentException("a milestone names the item it awards");
        }
        return new Milestone(seals, json.get("item").getAsString());
    }
}
