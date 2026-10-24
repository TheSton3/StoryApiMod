package com.thestone.storyapimod.event;

import com.thestone.storyapimod.Storyapimod;
import com.thestone.storyapimod.story.StoryRuntime;
import com.thestone.storyapimod.story.triggers.TriggerSignal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = Storyapimod.MOD_ID)
public final class StoryGameplayEvents {
    private static final Map<UUID, List<Runnable>> PENDING = new HashMap<>();

    private StoryGameplayEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getSource().getEntity() instanceof ServerPlayer player) {
            defer(player, () -> {
                if (!event.isCanceled() && mob.isDeadOrDying()) StoryRuntime.signal(player, new TriggerSignal("entity_killed", null, mob, ItemStack.EMPTY, 1, null, null));
            });
        }
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            ItemStack original = event.getOriginalStack();
            int amount = original.getCount() - event.getCurrentStack().getCount();
            if (amount > 0) item(player, "item_picked_up", original, amount, null);
        }
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) item(player, "item_crafted", event.getCrafting(), event.getCrafting().getCount(), null);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && event.getLevel() instanceof ServerLevel level) {
            BlockPos pos = event.getPos().immutable();
            BlockState state = event.getState();
            defer(player, () -> {
                if (!event.isCanceled() && player.level() == level && level.hasChunkAt(pos) && !level.getBlockState(pos).is(state.getBlock())) {
                    StoryRuntime.signal(player, new TriggerSignal("block_broken", state, null, ItemStack.EMPTY, 1, null, null));
                }
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItem().copy();
            defer(player, () -> {
                if (!event.isCanceled() && event.getDuration() > 0) item(player, "item_used", stack, 1, "start");
            });
        }
    }

    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) item(player, "item_used", event.getItem(), 1, "finish");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onUseRelease(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItem().copy();
            defer(player, () -> {
                if (!event.isCanceled()) item(player, "item_used", stack, 1, "release");
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getItemStack().copy();
            defer(player, () -> {
                if (!event.isCanceled()) item(player, "item_used", stack, 1, "click");
            });
        }
    }

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isSleeping()) {
            StoryRuntime.signal(player, TriggerSignal.simple("sleep_ended"));
        }
    }

    private static void item(ServerPlayer player, String type, ItemStack stack, int count, String phase) {
        if (!stack.isEmpty() && count > 0) StoryRuntime.signal(player, new TriggerSignal(type, null, null, stack.copy(), count, null, phase));
    }

    private static void defer(ServerPlayer player, Runnable action) {
        PENDING.computeIfAbsent(player.getUUID(), key -> new ArrayList<>()).add(action);
    }

    public static void flush(ServerPlayer player) {
        List<Runnable> actions = PENDING.remove(player.getUUID());
        if (actions != null) actions.forEach(Runnable::run);
    }

    public static void forget(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    public static void clear() {
        PENDING.clear();
    }
}
