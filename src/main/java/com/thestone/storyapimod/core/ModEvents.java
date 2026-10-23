package com.thestone.storyapimod.event;

import com.thestone.storyapimod.Storyapimod;
import com.thestone.storyapimod.payloads.Server2Client;
import com.thestone.storyapimod.payloads.StoryProgressPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = Storyapimod.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
                StoryProgressPayload.TYPE,
                StoryProgressPayload.STREAM_CODEC,
                Server2Client::handleProgress
        );
    }
}
