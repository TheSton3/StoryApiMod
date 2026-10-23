package com.thestone.storyapimod.core;

import com.thestone.storyapimod.Storyapimod;
import com.thestone.storyapimod.command.QuestResetCommand;
import com.thestone.storyapimod.payloads.DialoguePayload;
import com.thestone.storyapimod.payloads.Server2Client;
import com.thestone.storyapimod.payloads.StoryProgressPayload;
import com.thestone.storyapimod.payloads.QuestCatalogPayload;
import com.thestone.storyapimod.command.StoryCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.command.ConfigCommand;

@EventBusSubscriber(modid = Storyapimod.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1")
                .executesOn(HandlerThread.MAIN);
        registrar.playToClient(QuestCatalogPayload.TYPE, QuestCatalogPayload.STREAM_CODEC, Server2Client::handleCatalog);
        registrar.playToClient(
                StoryProgressPayload.TYPE,
                StoryProgressPayload.STREAM_CODEC,
                Server2Client::handleProgress
        );
        registrar.playToClient(
                DialoguePayload.TYPE,
                DialoguePayload.STREAM_CODEC,
                Server2Client::handleSubtitles
        );

    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {

        QuestResetCommand.register(event.getDispatcher());
        StoryCommand.register(event.getDispatcher());
        ConfigCommand.register(event.getDispatcher());
    }
}
