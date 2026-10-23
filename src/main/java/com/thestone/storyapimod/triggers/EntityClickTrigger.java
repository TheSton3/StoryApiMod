package com.thestone.storyapimod.triggers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class EntityClickTrigger extends StoryTrigger {
    private final Predicate<Entity> condition;
    private final BiConsumer<ServerPlayer, Entity> action;

    public EntityClickTrigger(ResourceLocation id, boolean once, Predicate<Entity> condition, BiConsumer<ServerPlayer, Entity> action) {
        super(id, once);
        this.condition = condition;
        this.action = action;
    }

    public void onClick(ServerPlayer player, Entity entity) {
        if (condition.test(entity)) {
            execute(player, () -> action.accept(player, entity));
        }
    }
}