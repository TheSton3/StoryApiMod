package com.thestone.storyapimod.subtitles;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

public class DialogueSubtitles {


    public static class DialogueLine {
        private final Component text;
        private final Style style;
        private final int totalDuration;
        private final int fadeInDuration;
        private final int fadeOutDuration;
        private int remainingTicks;
        private final Consumer<DialogueLine> onFinish;

        public DialogueLine(Component text, Style style, int duration, int fadeIn, int fadeOut, Consumer<DialogueLine> onFinish) {
            this.text = text;
            this.style = style;
            this.totalDuration = duration;
            this.fadeInDuration = fadeIn;
            this.fadeOutDuration = fadeOut;
            this.remainingTicks = duration;
            this.onFinish = onFinish;
        }


        public float getProgress() {
            return 1.0f - (float) remainingTicks / totalDuration;
        }


        public float getAlpha() {
            float progress = getProgress();


            if (progress < (float) fadeInDuration / totalDuration) {
                return progress * totalDuration / fadeInDuration;
            } else if (remainingTicks <= fadeOutDuration) {
                return (float) remainingTicks / fadeOutDuration;
            } else {
                return 1.0f;
            }
        }

        public boolean isFinished() {
            return remainingTicks <= 0;
        }

        public void tick() {
            if (remainingTicks > 0) {
                remainingTicks--;
            }
        }

        public Component getStyledText() {
            return text.copy().withStyle(style);
        }

        public void triggerFinish() {
            if (onFinish != null) {
                onFinish.accept(this);
            }
        }
    }


    private static final DialogueSubtitles INSTANCE = new DialogueSubtitles();
    private final Deque<DialogueLine> queue = new ArrayDeque<>();
    private DialogueLine currentLine = null;
    private boolean paused = false;

    private DialogueSubtitles() {

    }

    public static DialogueSubtitles get() {
        return INSTANCE;
    }


    public void enqueue(Component text, Style style, int durationTicks, Consumer<DialogueLine> onFinish) {
        DialogueLine line = new DialogueLine(text, style, durationTicks, 10, 20, onFinish);


        if (currentLine == null && queue.isEmpty()) {
            currentLine = line;
        } else {
            queue.add(line);
        }
    }


    public void enqueue(Component text, Style style, int durationTicks) {
        enqueue(text, style, durationTicks, null);
    }


    public void clear() {
        queue.clear();
        currentLine = null;
    }


    public void setPaused(boolean paused) {
        this.paused = paused;
    }


    public void tick() {
        if (paused || currentLine == null) {
            return;
        }
        currentLine.tick();

        if (currentLine.isFinished()) {
            currentLine.triggerFinish();
            currentLine = queue.poll();
        }
    }


    public void render(GuiGraphics gui) {
        if (currentLine == null || currentLine.getAlpha() <= 0.01f) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        String text = currentLine.getStyledText().getString();
        int textWidth = mc.font.width(text);


        int x = (mc.getWindow().getGuiScaledWidth() - textWidth) / 2;

        int y = mc.getWindow().getGuiScaledHeight() - 40;


        int alpha = Mth.clamp((int) (255 * currentLine.getAlpha()), 0, 255);
        int brightness = Mth.floor(Mth.clampedLerp(255.0, 75.0, currentLine.getProgress()));
        int color = (alpha << 24) | (brightness << 16) | (brightness << 8) | brightness;
        int background = mc.options.getBackgroundColor(0.8F);
        int backgroundAlpha = Mth.clamp((int) ((background >>> 24) * currentLine.getAlpha()), 0, 255);
        gui.fill(x - 2, y - 21, x + textWidth + 2, y - 11, (background & 0xFFFFFF) | (backgroundAlpha << 24));


        gui.drawString(mc.font, text, x, y - 20, color, true);
    }


    public boolean isVisible() {
        return currentLine != null && currentLine.getAlpha() > 0.01f;
    }

    public boolean isPlaying() {
        return currentLine != null || !queue.isEmpty();
    }


    public void show(String message, int ticks) {
        clear();
        enqueue(Component.literal(message), Style.EMPTY.withColor(ChatFormatting.GOLD).withItalic(true), ticks);
    }

}
