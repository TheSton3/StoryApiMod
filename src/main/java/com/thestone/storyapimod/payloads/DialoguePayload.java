package com.thestone.storyapi.data;

import com.thestone.storyapi.StoryApi;
import com.thestone.storyapi.subtitles.DialogueSubtitles;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;



public record DialoguePacket(String dialogueId,
                             String componentJson,
                             int durationTicks,
                             boolean clearQueue,
                             boolean isFinal) implements CustomPacketPayload {

    public static final Type<DialoguePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(StoryApi.MOD_ID, "dialogue_packet"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, DialoguePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            DialoguePacket::dialogueId,
            ByteBufCodecs.STRING_UTF8,
            DialoguePacket::componentJson,
            ByteBufCodecs.VAR_INT,
            DialoguePacket::durationTicks,
            ByteBufCodecs.BOOL,
            DialoguePacket::clearQueue,
            ByteBufCodecs.BOOL,
            DialoguePacket::isFinal,
            DialoguePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendTo(ServerPlayer player, Component component, int duration, boolean clear, boolean isFinal) {

        String json = Component.Serializer.toJson(component, RegistryAccess.EMPTY);


        player.connection.send(new DialoguePacket(
                "dialogue",
                json,
                duration,
                clear,
                isFinal
        ));
    }

    public void handle(IPayloadContext context) {
        if (!context.flow().isClientbound()) return;
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                return;
            }
            Component component = Component.Serializer.fromJson(this.componentJson, RegistryAccess.EMPTY);
            if (component == null) {
                return;
            }
            DialogueSubtitles subtitles = DialogueSubtitles.get();
            if (this.clearQueue) {
                subtitles.clear();
            }
            subtitles.enqueue(
                    component,
                    component.getStyle(),
                    this.durationTicks
            );
        });
    }
}