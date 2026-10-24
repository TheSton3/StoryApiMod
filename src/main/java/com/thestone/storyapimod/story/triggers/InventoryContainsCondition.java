package com.thestone.storyapimod.story.triggers;

import com.thestone.storyapimod.story.StoryDocument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record InventoryContainsCondition(StoryDocument.Trigger definition) implements TriggerCondition {
    public boolean matches(ServerPlayer player, TriggerSignal signal, TriggerMemory memory) {
        if (signal != TriggerSignal.TICK) return false;
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (TriggerConditions.itemMatches(definition, stack)) count += stack.getCount();
        }
        return count >= definition.count();
    }
}
