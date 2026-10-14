package dev.nostalgiacraft.auditor.diploma;

import dev.nostalgiacraft.auditor.registry.ModItems;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public enum Diploma {
    JUNIOR(() -> ModItems.JUNIOR_DIPLOMA.get()),
    SENIOR(() -> ModItems.SENIOR_DIPLOMA.get()),
    MASTER(() -> ModItems.MASTER_DIPLOMA.get());

    private static final char FIRST_GLYPH = '';
    private final Supplier<Item> item;

    Diploma(Supplier<Item> item) {
        this.item = item;
    }

    public Item item() {
        return item.get();
    }

    public char glyph() {
        return (char) (FIRST_GLYPH + ordinal());
    }
}
