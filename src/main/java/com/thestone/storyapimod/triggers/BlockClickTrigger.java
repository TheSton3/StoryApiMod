package com.thestone.storyapimod.triggers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class BlockClickTrigger extends StoryTrigger {
    private final Predicate<BlockState> condition;
    private final BiConsumer<ServerPlayer, BlockState> action;

    public BlockClickTrigger(ResourceLocation id, boolean once, Predicate<BlockState> condition, BiConsumer<ServerPlayer, BlockState> action) {
        super(id, once);
        this.condition = condition;
        this.action = action;

    }

    public void onClick(ServerPlayer player, BlockState state) {
        if (condition.test(state)) {
            execute(player, () -> action.accept(player, state));
        }
    }
}
