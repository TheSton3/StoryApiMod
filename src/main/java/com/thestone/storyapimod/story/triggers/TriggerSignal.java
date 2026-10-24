package com.thestone.storyapimod.story.triggers;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public record TriggerSignal(String type, BlockState block, Entity entity, ItemStack item, int amount, String from, String phase) {
    public static final TriggerSignal TICK = new TriggerSignal("tick", null, null, ItemStack.EMPTY, 1, null, null);

    public static TriggerSignal simple(String type) {
        return new TriggerSignal(type, null, null, ItemStack.EMPTY, 1, null, null);
    }
}
