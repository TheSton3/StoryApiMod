package com.thestone.storyapimod.core;

import com.thestone.storyapimod.Storyapimod;
import com.thestone.storyapimod.quest.Quest;
import com.thestone.storyapimod.quest.QuestStep;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class ModQuests {
    public static final ResourceLocation FIND_CHEST = id("find_chest");
    public static final ResourceLocation ENTER_VILLAGE = id("enter_village");
    public static final ResourceLocation RETURN_TO_GUIDE = id("return_to_guide");

    public static final Quest VILLAGE_INTRO = new Quest(
            id("village_intro"),
            Component.literal("Знакомство с деревней"),
            List.of(
                    new QuestStep(FIND_CHEST, Component.literal("Нажми ПКМ по сундуку.")),
                    new QuestStep(ENTER_VILLAGE, Component.literal("Войди в деревню.")),
                    new QuestStep(RETURN_TO_GUIDE, Component.literal("Вернись к проводнику."))
            ),
            player -> {
                ItemStack reward = new ItemStack(Items.EMERALD, 3);
                if (!player.getInventory().add(reward)) {
                    player.drop(reward, false);
                }
            }
    );

    public static final List<Quest> ALL = List.of(VILLAGE_INTRO);

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Storyapimod.MOD_ID, path);
    }
}
