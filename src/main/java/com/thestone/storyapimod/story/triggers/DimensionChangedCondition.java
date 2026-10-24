package com.thestone.storyapimod.story.triggers;

import com.thestone.storyapimod.story.StoryDocument;
import net.minecraft.server.level.ServerPlayer;

public record DimensionChangedCondition(StoryDocument.Trigger definition) implements TriggerCondition {
    public boolean matches(ServerPlayer player, TriggerSignal signal, TriggerMemory memory) {
        return signal.type().equals("dimension_changed") && (definition.from() == null || definition.from().equals(signal.from()));
    }
}
