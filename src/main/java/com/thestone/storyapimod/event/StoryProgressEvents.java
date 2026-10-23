package com.thestone.storyapimod.event;

import com.thestone.storyapimod.Storyapimod;
import com.thestone.storyapimod.util.StoryProgress;
import com.thestone.storyapimod.story.StoryRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Storyapimod.MOD_ID)
public class StoryProgressEvents {
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StoryRuntime.sync(player);
            StoryProgress.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StoryProgress.sync(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StoryProgress.sync(player);
        }
    }
}
