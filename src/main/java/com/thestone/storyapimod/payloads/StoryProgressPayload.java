package com.thestone.storyapimod.payloads;

import com.thestone.storyapimod.Storyapimod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record StoryProgressPayload(List<String> completed) implements CustomPacketPayload {

    public static final Type<StoryProgressPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    Storyapimod.MOD_ID,
                    "story_progress"
            ));

    public static final StreamCodec<ByteBuf, StoryProgressPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    StoryProgressPayload::completed,
                    StoryProgressPayload::new
            );

    public StoryProgressPayload {
        completed = List.copyOf(completed);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
