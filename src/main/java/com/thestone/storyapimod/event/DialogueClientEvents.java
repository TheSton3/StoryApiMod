package com.thestone.storyapimod.event;

import com.thestone.storyapimod.Storyapimod;
import com.thestone.storyapimod.quest.ClientQuestCatalog;
import com.thestone.storyapimod.subtitles.DialogueSubtitles;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = Storyapimod.MOD_ID, value = Dist.CLIENT)
public class DialogueClientEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null || mc.isPaused()) {
            return;
        }

        DialogueSubtitles.get().tick();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }

        DialogueSubtitles.get().render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientQuestCatalog.clear();
        DialogueSubtitles subtitles = DialogueSubtitles.get();
        subtitles.clear();
        subtitles.setPaused(false);
    }
}
