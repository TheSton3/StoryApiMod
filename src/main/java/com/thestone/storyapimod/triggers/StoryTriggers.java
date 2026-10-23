package com.thestone.storyapimod.triggers;

import com.thestone.storyapimod.core.ModAttachments;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class StoryTriggers {
    private final Set<ResourceLocation> ids = new HashSet<>();

    private final List<BlockClickTrigger> blocks = new ArrayList<>();
    private final List<EntityClickTrigger> entities = new ArrayList<>();
    private final List<AreaEnterTrigger> areas = new ArrayList<>();

    public void register(BlockClickTrigger trigger) {
        reserve(trigger);
        blocks.add(trigger);
    }

    public void register(EntityClickTrigger trigger) {
        reserve(trigger);
        entities.add(trigger);
    }

    public void register(AreaEnterTrigger trigger) {
        reserve(trigger);
        areas.add(trigger);
    }

    public void onBlock(ResourceLocation id, boolean once, Predicate<BlockState> condition, BiConsumer<ServerPlayer, BlockState> action) {
        register(new BlockClickTrigger(id, once, condition, action));
    }

    public void onEntity(ResourceLocation id, boolean once, Predicate<Entity> condition, BiConsumer<ServerPlayer, Entity> action) {
        register(new EntityClickTrigger(id, once, condition, action));
    }

    public void onEnter(ResourceLocation id, ResourceKey<Level> dimension, AABB bounds, boolean once, Consumer<ServerPlayer> action) {
        register(new AreaEnterTrigger(id, dimension, bounds, once, action));
    }

    public boolean isCompleted(ServerPlayer player, ResourceLocation id) {
        return player.getData(ModAttachments.COMPLETED)
                .contains(id.toString());
    }

    public void blockClicked(ServerPlayer player, BlockState blockState) {
        for (BlockClickTrigger trigger : List.copyOf(blocks)) {
            trigger.onClick(player, blockState);
        }
    }

    public void entityClicked(ServerPlayer player, Entity entity) {
        for (EntityClickTrigger trigger : List.copyOf(entities)) {
            trigger.onClick(player, entity);
        }
    }

    public void tick(ServerPlayer player) {
        for (AreaEnterTrigger trigger : List.copyOf(areas)) {
            trigger.tick(player);
        }
    }

    public void forget(ServerPlayer player) {
        for (AreaEnterTrigger trigger : areas) {
            trigger.forget(player);
        }
    }

    public void clear() {
        for (AreaEnterTrigger trigger : areas) {
            trigger.clear();
        }

        ids.clear();
        blocks.clear();
        entities.clear();
        areas.clear();
    }

    private void reserve(StoryTrigger trigger) {
        if (!ids.add(trigger.getId())) {
            throw new IllegalArgumentException(
                    "Duplicate story trigger: " + trigger.getId()
            );
        }
    }
}
