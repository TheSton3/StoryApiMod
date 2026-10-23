package com.thestone.storyapimod.payloads;

import com.google.gson.Gson;
import com.thestone.storyapimod.Storyapimod;
import com.thestone.storyapimod.quest.Quest;
import com.thestone.storyapimod.quest.QuestStep;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record QuestCatalogPayload(String json) implements CustomPacketPayload {
    private static final Gson GSON = new Gson();
    public static final Type<QuestCatalogPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Storyapimod.MOD_ID, "quest_catalog"));
    public static final StreamCodec<ByteBuf, QuestCatalogPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, QuestCatalogPayload::json, QuestCatalogPayload::new);

    public QuestCatalogPayload {
        if (json == null || json.length() > 32767) {
            throw new IllegalArgumentException("Quest catalog exceeds 32767 characters");
        }
    }

    public static QuestCatalogPayload from(List<Quest> quests) {
        List<Entry> entries = quests.stream().map(quest -> new Entry(quest.id().toString(), quest.title().getString(), quest.steps().stream()
                .map(step -> new Goal(step.id().toString(), step.description().getString())).toList())).toList();
        return new QuestCatalogPayload(GSON.toJson(entries));
    }

    public List<Quest> readQuests() {
        Entry[] entries = GSON.fromJson(json, Entry[].class);
        return Arrays.stream(entries).map(entry -> new Quest(ResourceLocation.parse(entry.id()), Component.literal(entry.title()), entry.steps().stream()
                .map(step -> new QuestStep(ResourceLocation.parse(step.id()), Component.literal(step.description()))).toList(), player -> {})).toList();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private record Entry(String id, String title, List<Goal> steps) {}

    private record Goal(String id, String description) {}
}
