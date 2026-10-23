package com.thestone.storyapimod.triggers;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class AreaEnterTrigger extends StoryTrigger {
    private final ResourceKey<Level> dimension;
    private final AABB bounds;
    private final Consumer<ServerPlayer> action;

    private final Set<UUID> playersInside = new HashSet<>();

    public AreaEnterTrigger(ResourceLocation id, ResourceKey<Level> dimension, AABB bounds, boolean once, Consumer<ServerPlayer> action) {
        super(id, once);
        this.dimension = dimension;
        this.bounds = bounds;
        this.action = action;
    }

    public void tick(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            forget(player);
            return;
        }

        boolean inside = player.level().dimension().equals(dimension)
                && bounds.contains(player.position());

        if (!inside) {
            forget(player);
            return;
        }

        if (playersInside.add(player.getUUID())) {
            execute(player, () -> action.accept(player));
        }
    }

    public void forget(ServerPlayer player) {
        playersInside.remove(player.getUUID());
    }

    public void clear() {
        playersInside.clear();
    }
}