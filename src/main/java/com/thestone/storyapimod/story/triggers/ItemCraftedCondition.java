package com.thestone.storyapimod.story.triggers;

import com.thestone.storyapimod.story.StoryDocument;
import net.minecraft.server.level.ServerPlayer;

public record ItemCraftedCondition(StoryDocument.Trigger definition) implements TriggerCondition {
    public boolean matches(ServerPlayer player, TriggerSignal signal, TriggerMemory memory) {
        return signal.type().equals("item_crafted") && TriggerConditions.itemMatches(definition, signal.item());
    }
}
