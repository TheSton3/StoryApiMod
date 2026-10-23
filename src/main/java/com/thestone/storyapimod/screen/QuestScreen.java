package com.thestone.storyapimod.screen;

import com.thestone.storyapimod.quest.ClientQuestCatalog;
import com.thestone.storyapimod.quest.Quest;
import com.thestone.storyapimod.quest.QuestManager;
import com.thestone.storyapimod.quest.QuestStep;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class QuestScreen extends Screen {
    private static final int LINE_HEIGHT = 13;

    private int panelLeft;
    private int panelWidth;
    private int contentTop;
    private int contentBottom;

    private double scroll;
    private int maxScroll;

    public QuestScreen() {
        super(Component.literal("Журнал квестов"));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(420, width - 20);
        panelLeft = (width - panelWidth) / 2;

        contentTop = 42;
        contentBottom = height - 42;

        addRenderableWidget(
                Button.builder(
                        Component.literal("Закрыть"),
                        button -> onClose()
                ).bounds(
                        width / 2 - 60,
                        height - 30,
                        120,
                        20
                ).build()
        );
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);

        gui.fill(
                panelLeft, 8,
                panelLeft + panelWidth, height - 36,
                0xEE171A21
        );

        gui.drawCenteredString(
                font,
                title,
                width / 2,
                18,
                0xFFFFD580
        );

        gui.fill(
                panelLeft + 10, 33,
                panelLeft + panelWidth - 10, 34,
                0xFF65583B
        );

        List<FormattedCharSequence> lines = buildLines();

        int visibleHeight = Math.max(1, contentBottom - contentTop);
        int totalHeight = lines.size() * LINE_HEIGHT;

        maxScroll = Math.max(0, totalHeight - visibleHeight);
        scroll = Mth.clamp(scroll, 0.0, (double) maxScroll);

        gui.enableScissor(
                panelLeft + 8,
                contentTop,
                panelLeft + panelWidth - 8,
                contentBottom
        );

        int y = contentTop - (int) scroll;

        for (FormattedCharSequence line : lines) {
            if (y + LINE_HEIGHT > contentTop && y < contentBottom) {
                gui.drawString(
                        font,
                        line,
                        panelLeft + 14,
                        y,
                        0xFFFFFFFF,
                        false
                );
            }

            y += LINE_HEIGHT;
        }

        gui.disableScissor();

        if (maxScroll > 0) {
            int barX = panelLeft + panelWidth - 7;
            int thumbHeight = Math.min(
                    visibleHeight,
                    Math.max(16, visibleHeight * visibleHeight / totalHeight)
            );

            int thumbY = contentTop + (int) (
                    scroll / maxScroll * (visibleHeight - thumbHeight)
            );

            gui.fill(
                    barX, contentTop,
                    barX + 3, contentBottom,
                    0xFF303540
            );

            gui.fill(
                    barX, thumbY,
                    barX + 3, thumbY + thumbHeight,
                    0xFFB9A575
            );
        }

    }

    private List<FormattedCharSequence> buildLines() {
        List<FormattedCharSequence> lines = new ArrayList<>();

        if (minecraft == null || minecraft.player == null) {
            return lines;
        }

        Player player = minecraft.player;
        boolean hasQuests = false;

        for (Quest quest : ClientQuestCatalog.quests()) {
            if (!QuestManager.isStarted(player, quest)
                    && !QuestManager.isCompleted(player, quest)) {
                continue;
            }

            hasQuests = true;

            boolean completed = QuestManager.isCompleted(player, quest);

            addText(
                    lines,
                    quest.title().copy()
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            );

            long doneCount = quest.steps().stream()
                    .filter(step ->
                            QuestManager.isStepCompleted(player, quest, step)
                    )
                    .count();

            addText(
                    lines,
                    Component.literal(
                            (completed ? "Завершён" : "В процессе")
                                    + " — " + doneCount
                                    + "/" + quest.steps().size()
                    ).withStyle(
                            completed
                                    ? ChatFormatting.GREEN
                                    : ChatFormatting.GRAY
                    )
            );

            Optional<QuestStep> current = QuestManager.currentStep(player, quest);

            for (QuestStep step : quest.steps()) {
                boolean done = QuestManager.isStepCompleted(
                        player, quest, step
                );

                boolean active = current
                        .map(value -> value.id().equals(step.id()))
                        .orElse(false);

                if (done) {
                    addText(
                            lines,
                            Component.literal("[x] ")
                                    .withStyle(ChatFormatting.GREEN)
                                    .append(
                                            step.description().copy()
                                                    .withStyle(
                                                            ChatFormatting.GREEN,
                                                            ChatFormatting.STRIKETHROUGH
                                                    )
                                    )
                    );
                } else {
                    addText(
                            lines,
                            Component.literal(active ? "> " : "[ ] ")
                                    .append(step.description().copy())
                                    .withStyle(
                                            active
                                                    ? ChatFormatting.YELLOW
                                                    : ChatFormatting.GRAY
                                    )
                    );
                }
            }

            addText(lines, Component.literal(" "));
        }

        if (!hasQuests) {
            addText(
                    lines,
                    Component.literal("У тебя пока нет заданий.")
                            .withStyle(ChatFormatting.GRAY)
            );
        }

        return lines;
    }

    private void addText(List<FormattedCharSequence> lines, Component text) {
        lines.addAll(font.split(text, Math.max(1, panelWidth - 36)));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= panelLeft
                && mouseX < panelLeft + panelWidth
                && mouseY >= contentTop
                && mouseY < contentBottom) {

            scroll = Mth.clamp(
                    scroll - verticalAmount * LINE_HEIGHT * 3,
                    0.0,
                    (double) maxScroll
            );

            return true;
        }

        return super.mouseScrolled(
                mouseX, mouseY, horizontalAmount, verticalAmount
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
