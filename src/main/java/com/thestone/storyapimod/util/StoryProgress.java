package com.thestone.storyapimod.util;

import com.thestone.storyapimod.core.ModAttachments;
import com.thestone.storyapimod.payloads.StoryProgressPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;

public class StoryProgress {

    public static boolean isCompleted(Player player, ResourceLocation id) {
        return player.getData(ModAttachments.COMPLETED)
                .contains(id.toString());
    }

    public static boolean complete(ServerPlayer player, ResourceLocation id) {
        if (isCompleted(player, id)) {
            return false;
        }

        ArrayList<String> completed = new ArrayList<>(
                player.getData(ModAttachments.COMPLETED)
        );

        completed.add(id.toString());
        player.setData(ModAttachments.COMPLETED, completed);
        sync(player);

        return true;
    }

    public static void reset(ServerPlayer player, ResourceLocation id) {
        ArrayList<String> completed = new ArrayList<>(
                player.getData(ModAttachments.COMPLETED)
        );

        if (completed.remove(id.toString())) {
            player.setData(ModAttachments.COMPLETED, completed);
            sync(player);
        }
    }

    public static void resetAll(ServerPlayer player) {
        player.setData(ModAttachments.COMPLETED, new ArrayList<>());
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
                player,
                new StoryProgressPayload(
                        player.getData(ModAttachments.COMPLETED)
                )
        );
    }
}
