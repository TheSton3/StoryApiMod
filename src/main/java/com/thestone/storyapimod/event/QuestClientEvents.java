package com.thestone.storyapimod.event;

import com.thestone.storyapimod.Storyapimod;
import com.thestone.storyapimod.core.ModKeymappings;
import com.thestone.storyapimod.screen.QuestScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Storyapimod.MOD_ID, value = Dist.CLIENT
)
public class QuestClientEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        while (ModKeymappings.PRESS_OPEN_JOURNAL.get().consumeClick()) {
            if (mc.player != null && mc.level != null && mc.screen == null) {
                mc.setScreen(new QuestScreen());
            }
        }
    }

}
