package com.thestone.storyapimod;

import com.thestone.storyapimod.core.ModAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Storyapimod.MOD_ID)
public class Storyapimod {
    public static final String MOD_ID = "storyapimod";

    public Storyapimod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModAttachments.init(modEventBus);
    }
}
