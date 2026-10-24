package com.thestone.storyapimod.quest;

import com.thestone.storyapimod.core.ModAttachments;
import com.thestone.storyapimod.payloads.DialoguePayload;
import com.thestone.storyapimod.util.StoryProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public class QuestManager {
    public static boolean isStarted(Player player, Quest quest) {
        return StoryProgress.isCompleted(player, key(quest, "started"));
    }

    public static boolean isCompleted(Player player, Quest quest) {
        return StoryProgress.isCompleted(player, key(quest, "completed"));
    }

    public static boolean isActive(Player player, Quest quest) {
        return isStarted(player, quest) && !isCompleted(player, quest);
    }
    public static boolean isStepCompleted(Player player, Quest quest, QuestStep step) {
        return StoryProgress.isCompleted(player, stepKey(quest, step));
    }
    public static Optional<QuestStep> currentStep(Player player, Quest quest) {
        if (!isActive(player, quest)) {
            return Optional.empty();
        }

        return quest.steps().stream()
                .filter(step -> !StoryProgress.isCompleted(
                        player,
                        stepKey(quest, step)
                ))
                .findFirst();
    }

    public static boolean reset(ServerPlayer player, Quest quest) {
        java.util.HashSet<String> keys = new java.util.HashSet<>();

        keys.add(key(quest, "started").toString());
        keys.add(key(quest, "completed").toString());

        for (QuestStep step : quest.steps()) {
            keys.add(stepKey(quest, step).toString());
        }

        java.util.ArrayList<String> completed = new java.util.ArrayList<>(
                player.getData(ModAttachments.COMPLETED)
        );

        boolean changed = completed.removeIf(keys::contains);
        changed |= com.thestone.storyapimod.story.StoryRuntime.reset(player, quest);

        if (changed) {
            player.setData(ModAttachments.COMPLETED, completed);
            StoryProgress.sync(player);
        }

        return changed;
    }
    public static boolean start(ServerPlayer player, Quest quest) {
        if (isStarted(player, quest) || isCompleted(player, quest)) {
            return false;
        }

        StoryProgress.complete(player, key(quest, "started"));

        DialoguePayload.sendTo(
                player,
                Component.literal("Новое задание: ")
                        .append(quest.title())
                        .withStyle(ChatFormatting.GOLD),
                80,
                true,
                false
        );

        showCurrentStep(player, quest);
        return true;
    }

    public static boolean completeStep(ServerPlayer player, Quest quest, ResourceLocation stepId) {
        Optional<QuestStep> current = currentStep(player, quest);

        if (current.isEmpty() || !current.get().id().equals(stepId)) {
            return false;
        }

        QuestStep step = current.get();

        if (!StoryProgress.complete(player, stepKey(quest, step))) {
            return false;
        }

        DialoguePayload.sendTo(
                player,
                Component.literal("Выполнено: ")
                        .append(step.description())
                        .withStyle(ChatFormatting.GREEN),
                80,
                true,
                false
        );

        if (currentStep(player, quest).isPresent()) {
            showCurrentStep(player, quest);
        } else {
            finish(player, quest);
        }

        return true;
    }

    public static void showCurrentStep(ServerPlayer player, Quest quest) {
        currentStep(player, quest).ifPresent(step ->
                DialoguePayload.sendTo(
                        player,
                        Component.literal("Цель: ")
                                .append(step.description())
                                .withStyle(ChatFormatting.YELLOW),
                        120,
                        false,
                        true
                )
        );
    }

    private static void finish(ServerPlayer player, Quest quest) {
        if (!StoryProgress.complete(player, key(quest, "completed"))) {
            return;
        }

        quest.reward().accept(player);

        DialoguePayload.sendTo(
                player,
                Component.literal("Задание завершено: ")
                        .append(quest.title())
                        .withStyle(ChatFormatting.GOLD),
                100,
                false,
                true
        );
    }

    private static ResourceLocation key(Quest quest, String suffix) {
        return ResourceLocation.fromNamespaceAndPath(
                quest.id().getNamespace(),
                "quest/" + quest.id().getPath() + "/" + suffix
        );
    }

    private static ResourceLocation stepKey(Quest quest, QuestStep step) {
        return key(
                quest,
                "steps/" + step.id().getNamespace() + "/" + step.id().getPath()
        );
    }
}
