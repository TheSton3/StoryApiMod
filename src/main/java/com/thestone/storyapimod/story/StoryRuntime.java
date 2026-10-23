package com.thestone.storyapimod.story;

import com.mojang.logging.LogUtils;
import com.thestone.storyapimod.payloads.DialoguePayload;
import com.thestone.storyapimod.payloads.QuestCatalogPayload;
import com.thestone.storyapimod.quest.Quest;
import com.thestone.storyapimod.quest.QuestManager;
import com.thestone.storyapimod.quest.QuestStep;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

public final class StoryRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static List<Chain> chains = List.of();
    private static List<Quest> quests = List.of();
    private static List<ResolvedTrigger> areas = List.of();
    private static QuestCatalogPayload catalog = QuestCatalogPayload.from(List.of());
    private static final Map<UUID, Set<ResolvedTrigger>> OCCUPIED = new HashMap<>();

    private StoryRuntime() {}

    public static Path directory() {
        return FMLPaths.CONFIGDIR.get().resolve("storyapimod").resolve("stories");
    }

    public static List<Quest> quests() {
        return quests;
    }

    public static int reload(MinecraftServer server) throws IOException {
        Path directory = directory();
        if (Files.notExists(directory)) {
            Files.createDirectories(directory);
            try (InputStream input = StoryRuntime.class.getResourceAsStream("/story_examples/village.json")) {
                if (input == null) {
                    throw new IOException("Missing bundled example story");
                }
                Files.copy(input, directory.resolve("village.json"));
            }
        }

        List<StoryDocument> documents = StoryFiles.read(directory);
        List<Chain> loadedChains = new ArrayList<>();
        List<Quest> loadedQuests = new ArrayList<>();
        List<ResolvedTrigger> loadedAreas = new ArrayList<>();
        for (StoryDocument document : documents) {
            List<BoundQuest> storyQuests = new ArrayList<>();
            for (StoryDocument.QuestDefinition definition : document.quests()) {
                try {
                    Item rewardItem = null;
                    if (definition.reward() != null) {
                        ResourceLocation itemId = ResourceLocation.parse(definition.reward().item());
                        if (!BuiltInRegistries.ITEM.containsKey(itemId) || itemId.equals(ResourceLocation.withDefaultNamespace("air"))) {
                            throw new IllegalArgumentException("Unknown or empty reward item: " + itemId);
                        }
                        rewardItem = BuiltInRegistries.ITEM.get(itemId);
                    }
                    Item item = rewardItem;
                    List<QuestStep> steps = definition.steps().stream().map(step -> new QuestStep(ResourceLocation.parse(step.id()), Component.literal(step.description()))).toList();
                    Quest quest = new Quest(ResourceLocation.parse(definition.id()), Component.literal(definition.title()), steps,
                            player -> giveReward(player, item, definition.reward() == null ? 0 : definition.reward().count()));
                    ResolvedTrigger start = resolve(server, definition.start());
                    List<BoundStep> boundSteps = new ArrayList<>();
                    addArea(loadedAreas, start);
                    for (StoryDocument.StepDefinition step : definition.steps()) {
                        ResolvedTrigger trigger = resolve(server, step.trigger());
                        boundSteps.add(new BoundStep(ResourceLocation.parse(step.id()), trigger, step.dialogue()));
                        addArea(loadedAreas, trigger);
                    }
                    storyQuests.add(new BoundQuest(quest, start, List.copyOf(boundSteps), definition.onStart(), definition.onComplete()));
                    loadedQuests.add(quest);
                } catch (RuntimeException exception) {
                    throw new IOException(document.id() + " / " + definition.id() + ": " + exception.getMessage(), exception);
                }
            }
            loadedChains.add(new Chain(List.copyOf(storyQuests)));
        }

        QuestCatalogPayload loadedCatalog;
        try {
            loadedCatalog = QuestCatalogPayload.from(loadedQuests);
        } catch (RuntimeException exception) {
            throw new IOException("Cannot synchronize quest catalog: " + exception.getMessage(), exception);
        }
        chains = List.copyOf(loadedChains);
        quests = List.copyOf(loadedQuests);
        areas = List.copyOf(loadedAreas);
        catalog = loadedCatalog;
        OCCUPIED.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sync(player);
        }
        LOGGER.info("Loaded {} stories and {} quests from {}", chains.size(), quests.size(), directory);
        return quests.size();
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, catalog);
    }

    public static void blockClicked(ServerPlayer player, BlockState state) {
        process(player, trigger -> trigger.block() != null && state.is(trigger.block()));
    }

    public static void entityClicked(ServerPlayer player, Entity entity) {
        process(player, trigger -> trigger.entity() != null && entity.getType() == trigger.entity()
                && (trigger.tag() == null || entity.getTags().contains(trigger.tag())));
    }

    public static void tick(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            forget(player);
            return;
        }
        Set<ResolvedTrigger> inside = new HashSet<>();
        for (ResolvedTrigger area : areas) {
            if (player.level().dimension().equals(area.dimension()) && area.bounds().contains(player.position())) {
                inside.add(area);
            }
        }
        Set<ResolvedTrigger> previous = OCCUPIED.put(player.getUUID(), inside);
        Set<ResolvedTrigger> entered = new HashSet<>(inside);
        if (previous != null) {
            entered.removeAll(previous);
        }
        if (!entered.isEmpty()) {
            process(player, entered::contains);
        }
    }

    private static void process(ServerPlayer player, Predicate<ResolvedTrigger> matches) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        for (Chain chain : chains) {
            BoundQuest current = chain.quests().stream().filter(value -> !QuestManager.isCompleted(player, value.quest())).findFirst().orElse(null);
            if (current == null) {
                continue;
            }
            Quest quest = current.quest();
            if (!QuestManager.isStarted(player, quest)) {
                if (matches(player, current.start(), matches) && QuestManager.start(player, quest)) {
                    play(player, current.onStart());
                }
                continue;
            }
            QuestStep active = QuestManager.currentStep(player, quest).orElse(null);
            if (active == null) {
                continue;
            }
            BoundStep step = current.steps().stream().filter(value -> value.id().equals(active.id())).findFirst().orElseThrow();
            if (matches(player, step.trigger(), matches) && QuestManager.completeStep(player, quest, step.id())) {
                List<StoryDocument.Dialogue> dialogue = new ArrayList<>(step.dialogue());
                if (QuestManager.isCompleted(player, quest)) {
                    dialogue.addAll(current.onComplete());
                }
                play(player, dialogue);
            }
        }
    }

    private static boolean matches(ServerPlayer player, ResolvedTrigger trigger, Predicate<ResolvedTrigger> predicate) {
        return player.level().dimension().equals(trigger.dimension()) && predicate.test(trigger);
    }

    private static void play(ServerPlayer player, List<StoryDocument.Dialogue> dialogue) {
        for (int index = 0; index < dialogue.size(); index++) {
            StoryDocument.Dialogue line = dialogue.get(index);
            Component text = Component.literal(line.text()).withColor(Integer.parseInt(line.color().substring(1), 16));
            DialoguePayload.sendTo(player, text, line.duration(), index == 0, index == dialogue.size() - 1);
        }
    }

    private static void giveReward(ServerPlayer player, Item item, int count) {
        while (count > 0) {
            ItemStack stack = new ItemStack(item);
            int amount = Math.min(count, stack.getMaxStackSize());
            stack.setCount(amount);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            count -= amount;
        }
    }

    private static ResolvedTrigger resolve(MinecraftServer server, StoryDocument.Trigger trigger) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(trigger.dimension()));
        if (server.getLevel(dimension) == null) {
            throw new IllegalArgumentException("Unknown dimension: " + trigger.dimension());
        }
        Block block = null;
        EntityType<?> entity = null;
        AABB bounds = null;
        switch (trigger.type()) {
            case "block_click" -> {
                ResourceLocation id = ResourceLocation.parse(trigger.block());
                if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                    throw new IllegalArgumentException("Unknown block: " + id);
                }
                block = BuiltInRegistries.BLOCK.get(id);
            }
            case "entity_click" -> {
                ResourceLocation id = ResourceLocation.parse(trigger.entity());
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                    throw new IllegalArgumentException("Unknown entity: " + id);
                }
                entity = BuiltInRegistries.ENTITY_TYPE.get(id);
            }
            case "area_enter" -> bounds = new AABB(trigger.min().get(0), trigger.min().get(1), trigger.min().get(2), trigger.max().get(0), trigger.max().get(1), trigger.max().get(2));
            default -> throw new IllegalArgumentException("Unknown trigger: " + trigger.type());
        }
        return new ResolvedTrigger(dimension, block, entity, trigger.tag(), bounds);
    }

    private static void addArea(List<ResolvedTrigger> target, ResolvedTrigger trigger) {
        if (trigger.bounds() != null) {
            target.add(trigger);
        }
    }

    public static void forget(ServerPlayer player) {
        OCCUPIED.remove(player.getUUID());
    }

    public static void clear() {
        chains = List.of();
        quests = List.of();
        areas = List.of();
        catalog = QuestCatalogPayload.from(List.of());
        OCCUPIED.clear();
    }

    private record Chain(List<BoundQuest> quests) {}

    private record BoundQuest(Quest quest, ResolvedTrigger start, List<BoundStep> steps, List<StoryDocument.Dialogue> onStart, List<StoryDocument.Dialogue> onComplete) {}

    private record BoundStep(ResourceLocation id, ResolvedTrigger trigger, List<StoryDocument.Dialogue> dialogue) {}

    private record ResolvedTrigger(ResourceKey<Level> dimension, Block block, EntityType<?> entity, String tag, AABB bounds) {}
}
