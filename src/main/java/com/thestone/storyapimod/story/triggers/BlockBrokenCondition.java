package com.thestone.storyapimod.story.triggers;

import com.thestone.storyapimod.story.StoryDocument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;

public record BlockBrokenCondition(StoryDocument.Trigger definition) implements TriggerCondition {
    public boolean matches(ServerPlayer player, TriggerSignal signal, TriggerMemory memory) {
        return signal.type().equals("block_broken") && signal.block() != null && BuiltInRegistries.BLOCK.getKey(signal.block().getBlock()).toString().equals(definition.block());
    }
}
