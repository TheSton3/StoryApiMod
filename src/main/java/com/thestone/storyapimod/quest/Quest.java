package com.thestone.storyapimod.quest;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public record Quest(ResourceLocation id, Component title, List<QuestStep> steps, Consumer<ServerPlayer> reward) {
    public Quest {
        Objects.requireNonNull(id);
        Objects.requireNonNull(title);
        Objects.requireNonNull(reward);

        steps = List.copyOf(steps);

        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Quest must have steps: " + id);
        }

        HashSet<ResourceLocation> ids = new HashSet<>();

        for (QuestStep step : steps) {
            if (!ids.add(step.id())) {
                throw new IllegalArgumentException(
                        "Duplicate step in quest " + id + ": " + step.id()
                );
            }
        }
    }
}