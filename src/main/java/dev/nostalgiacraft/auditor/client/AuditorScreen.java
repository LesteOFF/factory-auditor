package dev.nostalgiacraft.auditor.client;

import dev.nostalgiacraft.auditor.AuditorTexts;
import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.auditor.block.AuditorSettings;
import dev.nostalgiacraft.auditor.block.TransferMode;
import dev.nostalgiacraft.auditor.diploma.Diploma;
import dev.nostalgiacraft.auditor.network.AuditorPayloads;
import dev.nostalgiacraft.auditor.network.BoardSnapshot;
import dev.nostalgiacraft.auditor.rate.CertificationRun;
import dev.nostalgiacraft.auditor.seal.SealItem;
import dev.nostalgiacraft.auditor.seal.SealTier;
import dev.nostalgiacraft.auditor.tab.TabMedals;
import dev.nostalgiacraft.monitor.client.screen.WorkshopScreen;
import dev.nostalgiacraft.monitor.panel.ValueFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class AuditorScreen extends WorkshopScreen {
    private static final String[] TABS = {"auditor_overview", "auditor_certify", "auditor_board"};
    private static final int OVERVIEW = 0;
    private static final int CERTIFY = 1;
    private static final int LINE = 12;
    private static final int STEP = 24;
    private static final int GRAPH_TOP = 120;
    private static final int GRAPH_HEIGHT = 48;
    private static final int MIN_GRAPH_HEIGHT = 20;
    private static final int FOOTER_GAP = 8;
    private static final int PROGRESS_HEIGHT = 12;
    private final AuditorBlockEntity auditor;
    private final double handGoal;
    private final Item openedWith;
    private final BoardSnapshot board;
    private final Component boardItem;
    private final Set<Diploma> diplomas;
    private final int runMinutes;
    private int tab;
    private SealTier tier = SealTier.COPPER;
    private AuditorSettings shownSettings;
    private int shownTray;
    private boolean shownRunning;
    private boolean certifyRequested;

    public AuditorScreen(AuditorBlockEntity auditor, AuditorPayloads.OpenAuditor opened) {
        super(AuditorTexts.screen("title"), AuditorTexts.screen("help"));
        this.auditor = auditor;
        this.handGoal = opened.handGoal();
        this.openedWith = Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getMainHandItem().getItem();
        this.board = opened.board();
        ResourceLocation item = board.item().isEmpty() ? null : ResourceLocation.tryParse(board.item());
        this.boardItem = item == null ? null : BuiltInRegistries.ITEM.get(item).getDescription();
        this.diplomas = opened.diplomaSet();
        this.runMinutes = opened.runMinutes();
        remember();
    }

    private void remember() {
        shownSettings = auditor.settings();
        shownTray = auditor.traySize();
        shownRunning = auditor.run() != null;
    }

    @Override
    public void tick() {
        super.tick();
        if (auditor.isRemoved()) {
            onClose();
            return;
        }
        boolean changed = !auditor.settings().equals(shownSettings) || auditor.traySize() != shownTray
                || (auditor.run() != null) != shownRunning;
        if (changed) {
            certifyRequested = false;
            remember();
            rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        super.init();
        tabs(TABS, tab, selected -> {
            tab = selected;
            rebuildWidgets();
        });
        footerButtons();
        if (tab == OVERVIEW) {
            overviewWidgets();
        } else if (tab == CERTIFY) {
            certifyWidgets();
        }
    }

    private void footerButtons() {
        int x = layout.x();
        int third = (layout.contentWidth() - 12) / 3;
        int footer = layout.footerY();
        button(AuditorTexts.screen("advanced"), x, footer, third, () -> minecraft.setScreen(new AdvancedScreen(this, auditor)));
        button(AuditorTexts.screen("reset_total"), x + third + 6, footer, third,
                () -> PacketDistributor.sendToServer(new AuditorPayloads.ResetTotal(auditor.getBlockPos())));
        button(AuditorTexts.screen("tray", shownTray), x + 2 * (third + 6), footer, layout.contentWidth() - 2 * (third + 6),
                () -> PacketDistributor.sendToServer(new AuditorPayloads.TakeTray(auditor.getBlockPos()))).active = shownTray > 0;
    }

    private void overviewWidgets() {
        int x = layout.x();
        int y = layout.y();
        int width = layout.contentWidth();
        int half = (width - 6) / 2;
        TransferMode mode = auditor.settings().transfer();
        button(AuditorTexts.screen("mode.pass"), x, y + 60, half, mode == TransferMode.PASS, false, () -> changeMode(TransferMode.PASS));
        button(AuditorTexts.screen("mode.consume"), x + half + 6, y + 60, width - half - 6, mode == TransferMode.CONSUME, false,
                () -> changeMode(TransferMode.CONSUME));
        button(AuditorTexts.screen("faces.open"), x + width - 80, y + 96, 80, () -> minecraft.setScreen(new FacesScreen(this, auditor)));
    }

    private void certifyWidgets() {
        int x = layout.x();
        int y = layout.y();
        int width = layout.contentWidth();
        if (auditor.run() != null) {
            return;
        }
        if (auditor.settings().transfer() != TransferMode.CONSUME) {
            button(AuditorTexts.screen("certify.use_consume"), x + width - 110, y - 2, 110, () -> changeMode(TransferMode.CONSUME));
        }
        for (SealTier option : SealTier.values()) {
            addRenderableWidget(new MedalButton(x + option.ordinal() * (MedalButton.SIZE + 4), y + 2 * STEP + 12, TabMedals.medal(option),
                    SealItem.tierName(option), option == tier, () -> {
                tier = option;
                rebuildWidgets();
            }));
        }
        Button certify = button(AuditorTexts.screen("certify"), x, y + 5 * STEP + 4, width, true, false, this::startCertification);
        certify.active = readyToCertify() && !certifyRequested;
    }

    private void startCertification() {
        certifyRequested = true;
        PacketDistributor.sendToServer(new AuditorPayloads.StartCertification(auditor.getBlockPos(), tier.ordinal()));
        rebuildWidgets();
    }

    private boolean readyToCertify() {
        return auditor.settings().transfer() == TransferMode.CONSUME && !heldItem().isEmpty();
    }

    private ItemStack heldItem() {
        return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
    }

    private void changeMode(TransferMode mode) {
        if (auditor.settings().transfer() != mode) {
            AuditorView.send(auditor, auditor.settings().withTransfer(mode));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (tab == OVERVIEW) {
            drawOverview(graphics);
        } else if (tab == CERTIFY) {
            drawCertify(graphics);
        } else {
            drawBoard(graphics);
        }
    }

    private void drawOverview(GuiGraphics graphics) {
        int x = layout.x();
        int y = layout.y();
        int width = layout.contentWidth();
        ItemStack top = auditor.topItem();
        int textLeft = top.isEmpty() ? x : x + 22;
        if (!top.isEmpty()) {
            graphics.renderItem(top, x, y + 2);
        }
        String rate = AuditorView.rate(auditor.ratePerMinute());
        graphics.pose().pushPose();
        graphics.pose().scale(2, 2, 1);
        graphics.drawString(font, rate, textLeft / 2, y / 2, ACCENT, false);
        graphics.pose().popPose();
        graphics.drawString(font, AuditorTexts.screen("unit"), textLeft + font.width(rate) * 2 + 4, y + 7, MUTED, false);
        graphics.drawString(font, AuditorTexts.screen("total_value", ValueFormat.number(auditor.total())), x + width / 2, y + 4, INK, false);
        String status = AuditorView.statusLine(auditor).getString();
        graphics.drawString(font, font.plainSubstrByWidth(status, width), x, y + 22, AuditorView.statusColor(auditor.status()), false);
        graphics.drawString(font, AuditorTexts.screen("mode"), x, y + 48, MUTED, false);
        String mode = auditor.settings().transfer().name().toLowerCase(Locale.ROOT);
        graphics.drawString(font, font.plainSubstrByWidth(AuditorTexts.screen("mode." + mode + ".help").getString(), width), x, y + 84, MUTED, false);
        graphics.drawString(font, font.plainSubstrByWidth(AuditorView.facesSummary(auditor).getString(), width - 88), x, y + 102, INK, false);
        int graphHeight = Math.min(GRAPH_HEIGHT, layout.footerY() - FOOTER_GAP - (y + GRAPH_TOP));
        if (graphHeight >= MIN_GRAPH_HEIGHT) {
            AuditorView.drawGraph(graphics, font, auditor.history(), x, y + GRAPH_TOP, width, graphHeight);
        }
    }

    private void drawCertify(GuiGraphics graphics) {
        CertificationRun run = auditor.run();
        if (run != null) {
            drawRun(graphics, run);
            return;
        }
        int x = layout.x();
        int y = layout.y();
        int width = layout.contentWidth();
        step(graphics, AuditorTexts.screen("certify.step1"), auditor.settings().transfer() == TransferMode.CONSUME, x, y);
        ItemStack held = heldItem();
        Component item = held.isEmpty() ? AuditorTexts.screen("certify.no_item") : held.getHoverName();
        step(graphics, AuditorTexts.screen("certify.step2", item), !held.isEmpty(), x, y + STEP);
        graphics.drawString(font, AuditorTexts.screen("certify.step3", SealItem.tierName(tier)), x, y + 2 * STEP, INK, false);
        graphics.drawString(font, font.plainSubstrByWidth(goalLine().getString(), width), x, y + 3 * STEP + 14, ACCENT, false);
        graphics.drawString(font, font.plainSubstrByWidth(perksLine().getString(), width), x, y + 4 * STEP + 10, MUTED, false);
    }

    private Component goalLine() {
        if (handGoal <= 0 || heldItem().getItem() != openedWith) {
            return AuditorTexts.screen("certify.goal_unknown");
        }
        return AuditorTexts.screen("certify.goal", ValueFormat.number(tier.goal(handGoal)), runMinutes);
    }

    private Component perksLine() {
        if (diplomas.isEmpty()) {
            return AuditorTexts.screen("certify.no_perks");
        }
        String perks = diplomas.stream().map(diploma -> AuditorTexts.screen("perk." + diploma.name().toLowerCase(Locale.ROOT)).getString())
                .collect(Collectors.joining(", "));
        return AuditorTexts.screen("certify.perks", perks);
    }

    private void step(GuiGraphics graphics, Component text, boolean done, int x, int y) {
        graphics.blitSprite(done ? AuditorSprites.STEP_DONE : AuditorSprites.STEP_MISSING, x, y - 1, AuditorSprites.STEP_SIZE,
                AuditorSprites.STEP_SIZE);
        graphics.drawString(font, font.plainSubstrByWidth(text.getString(), layout.contentWidth() - 130), x + 12, y, INK, false);
    }

    private void drawRun(GuiGraphics graphics, CertificationRun run) {
        int x = layout.x();
        int y = layout.y();
        int width = layout.contentWidth();
        ResourceLocation id = ResourceLocation.tryParse(run.item());
        Component item = id == null ? Component.literal(run.item()) : BuiltInRegistries.ITEM.get(id).getDescription();
        graphics.drawString(font, AuditorTexts.screen("run.title", item, SealItem.tierName(SealTier.byIndex(run.tier()))), x, y, ACCENT, false);
        double share = Math.min(1, run.thisMinute() / Math.max(1, run.goal()));
        int barTop = y + STEP;
        graphics.blitSprite(AuditorSprites.PROGRESS, x, barTop, width, PROGRESS_HEIGHT);
        int filled = (int) Math.round(width * share);
        if (filled >= AuditorSprites.NINE_SLICE_MINIMUM) {
            graphics.blitSprite(share >= 1 ? AuditorSprites.PROGRESS_DONE : AuditorSprites.PROGRESS_FILL, x, barTop, filled, PROGRESS_HEIGHT);
        }
        graphics.drawString(font, AuditorTexts.screen("run.minute", run.minute() + 1, run.minutes(), ValueFormat.number(run.thisMinute()),
                ValueFormat.number(run.goal())), x, barTop + PROGRESS_HEIGHT + 6, INK, false);
        int seconds = auditor.runSeconds();
        graphics.drawString(font, AuditorTexts.screen("run.left", "%d:%02d".formatted(seconds / 60, seconds % 60)), x, barTop + 34, INK, false);
        graphics.drawString(font, AuditorTexts.screen("run.forgivable", run.forgivable()), x, barTop + 48, MUTED, false);
        graphics.drawWordWrap(font, AuditorTexts.screen("run.tip"), x, barTop + 66, width, MUTED);
    }

    private void drawBoard(GuiGraphics graphics) {
        int x = layout.x();
        int y = layout.y();
        int width = layout.contentWidth();
        if (boardItem == null) {
            graphics.drawString(font, AuditorTexts.screen("board.no_item"), x, y, MUTED, false);
        } else {
            graphics.drawString(font, AuditorTexts.screen("board.header", boardItem), x, y, ACCENT, false);
            if (board.lines().isEmpty()) {
                graphics.drawString(font, AuditorTexts.screen("board.empty"), x, y + LINE + 4, MUTED, false);
            }
            for (int index = 0; index < board.lines().size(); index++) {
                BoardSnapshot.Line line = board.lines().get(index);
                String rate = ValueFormat.number(line.rate()) + " /min";
                String name = font.plainSubstrByWidth((index + 1) + ". " + line.name(), width - font.width(rate) - 8);
                int top = y + (index + 1) * LINE + 4;
                graphics.drawString(font, name, x, top, index == 0 ? ACCENT : INK, false);
                graphics.drawString(font, rate, x + width - font.width(rate), top, MUTED, false);
            }
        }
        int bottom = layout.y() + layout.contentHeight() - LINE;
        graphics.drawString(font, AuditorTexts.screen("board.mine", board.seals(), TabMedals.titleName(board.titleValue())), x, bottom, INK, false);
    }
}
