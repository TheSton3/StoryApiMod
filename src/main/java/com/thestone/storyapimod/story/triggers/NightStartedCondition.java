package com.thestone.storyapimod.story.triggers;

import net.minecraft.server.level.ServerPlayer;

public final class NightStartedCondition implements TriggerCondition {
    public boolean matches(ServerPlayer player, TriggerSignal signal, TriggerMemory memory) {
        if (signal != TriggerSignal.TICK) return false;
        long time = Math.floorMod(player.level().getDayTime(), 24000);
        boolean night = player.level().dimensionType().hasSkyLight() && !player.level().dimensionType().hasFixedTime() && time >= 13000 && time < 23000;
        boolean started = Boolean.FALSE.equals(memory.previous) && night;
        memory.previous = night;
        return started;
    }
}
