package com.thestone.storyapimod.story.triggers;

import com.thestone.storyapimod.story.StoryDocument;
import net.minecraft.server.level.ServerPlayer;

public record ItemPickedUpCondition(StoryDocument.Trigger definition) implements TriggerCondition {
    public boolean matches(ServerPlayer player, TriggerSignal signal, TriggerMemory memory) {
        return signal.type().equals("item_picked_up") && TriggerConditions.itemMatches(definition, signal.item());
    }
}
