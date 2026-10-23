package com.thestone.storyapimod.payloads;


import com.thestone.storyapimod.Storyapimod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;


public record DialoguePayload(String dialogueId, String componentJson, int durationTicks, boolean clearQueue, boolean isFinal) implements CustomPacketPayload {

    public static final Type<DialoguePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Storyapimod.MOD_ID, "dialogue_packet"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, DialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            DialoguePayload::dialogueId,
            ByteBufCodecs.STRING_UTF8,
            DialoguePayload::componentJson,
            ByteBufCodecs.VAR_INT,
            DialoguePayload::durationTicks,
            ByteBufCodecs.BOOL,
            DialoguePayload::clearQueue,
            ByteBufCodecs.BOOL,
            DialoguePayload::isFinal,
            DialoguePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendTo(ServerPlayer player, Component component, int duration, boolean clear, boolean isFinal) {
        if (duration <= 0) {
            throw new IllegalArgumentException("Dialogue duration must be positive");
        }

        String json = Component.Serializer.toJson(
                component,
                player.registryAccess()
        );

        PacketDistributor.sendToPlayer(
                player,
                new DialoguePayload(
                        "dialogue",
                        json,
                        duration,
                        clear,
                        isFinal
                )
        );
    }
}
