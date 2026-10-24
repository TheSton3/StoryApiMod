package com.thestone.storyapimod.story.triggers;

import net.minecraft.server.level.ServerPlayer;

public final class SleepStartedCondition implements TriggerCondition {
    public boolean matches(ServerPlayer player, TriggerSignal signal, TriggerMemory memory) {
        if (signal != TriggerSignal.TICK) return false;
        boolean sleeping = player.isSleeping();
        boolean started = !Boolean.TRUE.equals(memory.previous) && sleeping;
        memory.previous = sleeping;
        return started;
    }
}
