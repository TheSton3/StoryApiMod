package com.thestone.storyapimod.quest;

import java.util.List;

public final class ClientQuestCatalog {
    private static List<Quest> quests = List.of();

    private ClientQuestCatalog() {}

    public static List<Quest> quests() {
        return quests;
    }

    public static void replace(List<Quest> values) {
        quests = List.copyOf(values);
    }

    public static void clear() {
        quests = List.of();
    }
}
