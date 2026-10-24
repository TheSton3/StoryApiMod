package com.thestone.storyapimod.event;

import com.thestone.storyapimod.Storyapimod;
import com.thestone.storyapimod.story.StoryRuntime;
import com.thestone.storyapimod.story.triggers.TriggerSignal;
import net.minecraft.world.item.ItemStack;
import java.io.IOException;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Storyapimod.MOD_ID)
public class StoryTriggerEvents {
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        StoryRuntime.clear();
        try {
            StoryRuntime.reload(event.getServer());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load stories: " + exception.getMessage(), exception);
        }
    }

    @SubscribeEvent
    public static void onBlockClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!canTrigger(player)) {
            return;
        }

        StoryRuntime.blockClicked(player, event.getLevel().getBlockState(event.getPos()));
    }

    @SubscribeEvent
    public static void onEntityClick(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!canTrigger(player)) {
            return;
        }
        StoryRuntime.entityClicked(player, event.getTarget());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!canTrigger(player)) {
            StoryRuntime.forget(player);
            return;
        }

        StoryGameplayEvents.flush(player);
        StoryRuntime.tick(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StoryRuntime.forget(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StoryRuntime.forget(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StoryRuntime.forget(player);
            StoryRuntime.signal(player, new TriggerSignal("dimension_changed", null, null, ItemStack.EMPTY, 1, event.getFrom().location().toString(), null));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        StoryRuntime.clear();
    }

    private static boolean canTrigger(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator();
    }
}
