package dev.nostalgiacraft.auditor.client;

import dev.nostalgiacraft.auditor.AuditorTexts;
import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.auditor.block.AuditorSettings;
import dev.nostalgiacraft.auditor.block.MeasureMode;
import dev.nostalgiacraft.monitor.client.screen.WorkshopScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class AdvancedScreen extends WorkshopScreen {
    private final Screen parent;
    private final AuditorBlockEntity auditor;
    private final List<NumberField> numberFields = new ArrayList<>();
    private String intervalText;
    private String toleranceText;
    private String limitText;
    private MeasureMode shownMeasure;

    private record NumberField(EditBox box, Predicate<Integer> valid) {
    }

    public AdvancedScreen(Screen parent, AuditorBlockEntity auditor) {
        super(AuditorTexts.screen("advanced.title"), AuditorTexts.screen("advanced.help"));
        this.parent = parent;
        this.auditor = auditor;
        AuditorSettings settings = auditor.settings();
        intervalText = String.valueOf(settings.interval());
        toleranceText = String.valueOf(settings.tolerance());
        limitText = String.valueOf(settings.limit());
        shownMeasure = settings.measure();
    }

    @Override
    protected void init() {
        super.init();
        numberFields.clear();
        int x = layout.x();
        int y = layout.y();
        int width = layout.contentWidth();
        List<Component> modes = Arrays.stream(MeasureMode.values()).map(mode -> (Component) AuditorTexts.option("measure", mode)).toList();
        segments(AuditorTexts.screen("measure"), modes, auditor.settings().measure().ordinal(), x, y, width,
                index -> AuditorView.send(auditor, auditor.settings().withMeasure(MeasureMode.values()[index])));
        paragraph(AuditorTexts.screen("measure.help"), x, y + 36, width);
        int each = (width - 12) / 3;
        numberField("interval", x, y + 64, each, intervalText, value -> intervalText = value, AuditorSettings::validInterval);
        numberField("tolerance", x + each + 6, y + 64, each, toleranceText, value -> toleranceText = value,
                value -> AuditorSettings.validTolerance(value, parsed(intervalText).orElse(auditor.settings().interval())));
        numberField("limit", x + 2 * (each + 6), y + 64, width - 2 * (each + 6), limitText, value -> limitText = value,
                AuditorSettings::validLimit);
        paragraph(AuditorTexts.screen("numbers.help"), x, y + 102, width);
        int half = (width - 6) / 2;
        button(AuditorTexts.screen("back"), x, layout.footerY(), half, this::onClose);
        button(AuditorTexts.screen("apply"), x + half + 6, layout.footerY(), width - half - 6, true, false, this::apply);
    }

    private void numberField(String key, int x, int y, int width, String value, Consumer<String> changed, Predicate<Integer> valid) {
        EditBox box = field(AuditorTexts.screen(key), x, y, width, value, 6, changed);
        box.setFilter(text -> text.chars().allMatch(Character::isDigit));
        box.setTooltip(Tooltip.create(AuditorTexts.screen(key + ".help")));
        numberFields.add(new NumberField(box, valid));
    }

    private static OptionalInt parsed(String text) {
        try {
            return OptionalInt.of(Integer.parseInt(text));
        } catch (NumberFormatException invalid) {
            return OptionalInt.empty();
        }
    }

    private boolean invalid(NumberField field) {
        OptionalInt value = parsed(field.box().getValue());
        return value.isEmpty() || !field.valid().test(value.getAsInt());
    }

    private void apply() {
        if (numberFields.stream().anyMatch(this::invalid)) {
            status = AuditorTexts.screen("invalid").getString();
            return;
        }
        status = "";
        AuditorView.send(auditor, auditor.settings().withNumbers(parsed(intervalText).orElseThrow(), parsed(toleranceText).orElseThrow(),
                parsed(limitText).orElseThrow()));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean enter = keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
        if (enter && numberFields.stream().anyMatch(field -> field.box().isFocused())) {
            apply();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (auditor.isRemoved()) {
            onClose();
        } else if (auditor.settings().measure() != shownMeasure) {
            shownMeasure = auditor.settings().measure();
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (NumberField field : numberFields) {
            if (invalid(field)) {
                EditBox box = field.box();
                graphics.blitSprite(AuditorSprites.FIELD_INVALID, box.getX() - 5, box.getY() - 4, box.getWidth() + 10, 20);
            }
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
