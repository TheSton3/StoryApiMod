package com.thestone.storyapimod.story.triggers;

import net.minecraft.server.level.ServerPlayer;

public final class SleepEndedCondition implements TriggerCondition {
    public boolean matches(ServerPlayer player, TriggerSignal signal, TriggerMemory memory) {
        return signal.type().equals("sleep_ended");
    }
}
