package com.thestone.storyapimod.quest;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record QuestStep(ResourceLocation id, Component description) {

    public QuestStep {
        Objects.requireNonNull(id);
        Objects.requireNonNull(description);
    }
}
