package com.thestone.storyapimod.triggers;

import com.thestone.storyapimod.Storyapimod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class ModTriggers {
    private static Player player = Minecraft.getInstance().player;

    public static void register(StoryTriggers triggers) {
        triggers.register(new EntityClickTrigger(
                id("guide_talk"),
                true,
                (entity) ->
                        entity.getType() == EntityType.VILLAGER
                                && entity.getTags().contains("story_guide"),
                (entity) -> entity.sendSystemMessage(
                        Component.literal("Проводник: добро пожаловать!")
                )
        ));

        triggers.register(new BlockClickTrigger(
                id("first_chest"),
                true,
                (state) -> state.is(Blocks.CHEST),
                (state) -> player.sendSystemMessage(
                        Component.literal("Ты нашёл сундук!")
                )
        ));

        // Вход в область — повторяется после выхода и возвращения.
        triggers.register(new AreaEnterTrigger(
                id("village_enter"),
                Level.OVERWORLD,
                new BlockPos(10, 60, 10),
                false,
                player -> player.sendSystemMessage(
                        Component.literal("Ты вошёл в деревню.")
                )
        ));
    }


    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                Storyapimod.MOD_ID,
                path
        );
    }
}
