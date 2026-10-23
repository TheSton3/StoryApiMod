package com.thestone.storyapimod.payloads;

import com.thestone.storyapimod.core.ModAttachments;
import com.thestone.storyapimod.quest.ClientQuestCatalog;
import com.thestone.storyapimod.subtitles.DialogueSubtitles;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;

public class Server2Client {
    public static void handleCatalog(QuestCatalogPayload payload, IPayloadContext context) {
        ClientQuestCatalog.replace(payload.readQuests());
    }

    public static void handleProgress(StoryProgressPayload payload, IPayloadContext context) {

        context.player().setData(
                ModAttachments.COMPLETED,
                new ArrayList<>(payload.completed())
        );
    }
    public static void handleSubtitles(DialoguePayload payload, IPayloadContext context) {
        if (!context.flow().isClientbound()) return;
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                return;
            }
            Component component = Component.Serializer.fromJson(
                    payload.componentJson(),
                    mc.level.registryAccess()
            );
            if (component == null) {
                return;
            }
            DialogueSubtitles subtitles = DialogueSubtitles.get();
            if (payload.clearQueue()) {
                subtitles.clear();
            }
            subtitles.enqueue(
                    component,
                    component.getStyle(),
                    payload.durationTicks()
            );
        });
    }
}
