package com.thestone.storyapimod.core;

import com.thestone.storyapimod.Storyapimod;
import com.thestone.storyapimod.quest.Quest;
import com.thestone.storyapimod.quest.QuestManager;
import com.thestone.storyapimod.triggers.AreaEnterTrigger;
import com.thestone.storyapimod.triggers.BlockClickTrigger;
import com.thestone.storyapimod.triggers.EntityClickTrigger;
import com.thestone.storyapimod.triggers.StoryTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public class ModTriggers {
    public static void register(StoryTriggers triggers) {
        triggers.register(new EntityClickTrigger(id("guide_talk"), false,
                entity -> entity.getType() == EntityType.VILLAGER && entity.getTags().contains("story_guide"),
                (player, entity) -> {
                    Quest quest = ModQuests.VILLAGE_INTRO;
                    if (QuestManager.start(player, quest)) {
                        return;
                    }
                    if (QuestManager.completeStep(player, quest, ModQuests.RETURN_TO_GUIDE)) {
                        return;
                    }
                    QuestManager.showCurrentStep(player, quest);
                }));

        triggers.register(new BlockClickTrigger(id("first_chest"), false,
                state -> state.is(Blocks.CHEST),
                (player, state) -> QuestManager.completeStep(player, ModQuests.VILLAGE_INTRO, ModQuests.FIND_CHEST)));

        triggers.register(new AreaEnterTrigger(id("village_enter"), Level.OVERWORLD,
                new AABB(10, -61, 10, 20, -50, 20), false,
                player -> QuestManager.completeStep(player, ModQuests.VILLAGE_INTRO, ModQuests.ENTER_VILLAGE)));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Storyapimod.MOD_ID, path);
    }
}
