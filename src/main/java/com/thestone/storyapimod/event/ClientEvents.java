package com.thestone.storyapimod.event;

import com.thestone.storyapimod.Storyapimod;
import com.thestone.storyapimod.core.ModKeymappings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = Storyapimod.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void registerKeybind(RegisterKeyMappingsEvent event) {
        event.register(ModKeymappings.PRESS_OPEN_JOURNAL.get());
    }
}
