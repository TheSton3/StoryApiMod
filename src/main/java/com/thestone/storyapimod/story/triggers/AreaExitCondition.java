package com.thestone.storyapimod.story.triggers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

public record AreaExitCondition(AABB bounds) implements TriggerCondition {
    public boolean matches(ServerPlayer player, TriggerSignal signal, TriggerMemory memory) {
        if (signal != TriggerSignal.TICK) return false;
        boolean inside = bounds.contains(player.position());
        boolean exited = Boolean.TRUE.equals(memory.previous) && !inside;
        memory.previous = inside;
        return exited;
    }
}
