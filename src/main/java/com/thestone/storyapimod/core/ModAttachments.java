package com.thestone.storyapimod;

import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Storyapimod.MOD_ID);


    public static final Supplier<AttachmentType<List<String>>> COMPLETED =
            ATTACHMENT_TYPES.register("completed_story_events", () ->
                    AttachmentType.<List<String>>builder(() -> new ArrayList<String>())
                            .serialize(Codec.STRING.listOf())
                            .copyOnDeath()
                            .build()
            );

    public static void init(IEventBus bus){
        ATTACHMENT_TYPES.register(bus);
    }


}
