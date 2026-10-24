package com.thestone.storyapimod.story.triggers;

import com.thestone.storyapimod.story.StoryDocument;
import com.thestone.storyapimod.story.StoryRuntime;
import net.minecraft.server.level.ServerPlayer;

public record EventCompletedCondition(StoryDocument.Trigger definition) implements TriggerCondition {
    public boolean matches(ServerPlayer player, TriggerSignal signal, TriggerMemory memory) {
        return signal == TriggerSignal.TICK && StoryRuntime.eventCompleted(player, definition.event());
    }
}
