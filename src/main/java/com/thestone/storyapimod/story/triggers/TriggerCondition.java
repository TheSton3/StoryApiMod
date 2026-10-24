package com.thestone.storyapimod.story.triggers;

import net.minecraft.server.level.ServerPlayer;

public interface TriggerCondition {
    boolean matches(ServerPlayer player, TriggerSignal signal, TriggerMemory memory);
}
