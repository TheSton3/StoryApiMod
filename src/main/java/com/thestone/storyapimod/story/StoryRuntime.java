package com.thestone.storyapimod.story;

import com.mojang.logging.LogUtils;
import com.thestone.storyapimod.payloads.DialoguePayload;
import com.thestone.storyapimod.payloads.QuestCatalogPayload;
import com.thestone.storyapimod.quest.Quest;
import com.thestone.storyapimod.quest.QuestManager;
import com.thestone.storyapimod.quest.QuestStep;
import com.thestone.storyapimod.core.ModAttachments;
import com.thestone.storyapimod.story.triggers.TriggerCondition;
import com.thestone.storyapimod.story.triggers.TriggerConditions;
import com.thestone.storyapimod.story.triggers.TriggerMemory;
import com.thestone.storyapimod.story.triggers.TriggerSignal;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

public final class StoryRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static List<Chain> chains = List.of();
    private static List<Quest> quests = List.of();
    private static QuestCatalogPayload catalog = QuestCatalogPayload.from(List.of());
    private static final Map<UUID, Map<String, TriggerMemory>> MEMORIES = new HashMap<>();

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
                    for (StoryDocument.StepDefinition step : definition.steps()) {
                        ResolvedTrigger trigger = resolve(server, step.trigger());
                        boundSteps.add(new BoundStep(ResourceLocation.parse(step.id()), trigger, step.dialogue(), step.actions()));
                    }
                    storyQuests.add(new BoundQuest(quest, start, List.copyOf(boundSteps), definition.onStart(), definition.onComplete(), definition.startActions(), definition.completionActions()));
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
        catalog = loadedCatalog;
        MEMORIES.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sync(player);
        }
        LOGGER.info("Loaded {} stories and {} quests from {}", chains.size(), quests.size(), directory);
        return quests.size();
    }

    public static void sync(ServerPlayer player) {
        execution(player).retain(quests.stream().map(quest -> quest.id().toString()).collect(java.util.stream.Collectors.toSet()));
        PacketDistributor.sendToPlayer(player, catalog);
    }

    public static void blockClicked(ServerPlayer player, BlockState state) {
        signal(player, new TriggerSignal("block_click", state, null, ItemStack.EMPTY, 1, null, null));
    }

    public static void entityClicked(ServerPlayer player, Entity entity) {
        signal(player, new TriggerSignal("entity_click", null, entity, ItemStack.EMPTY, 1, null, null));
    }

    public static void tick(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            forget(player);
            return;
        }
        execution(player).tick((owner, action) -> executeAction(player, owner, action));
        signal(player, TriggerSignal.TICK);
    }

    public static void signal(ServerPlayer player, TriggerSignal signal) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        for (Chain chain : chains) {
            if (chain.quests().stream().anyMatch(value -> execution(player).busy(value.quest().id().toString()))) continue;
            BoundQuest current = chain.quests().stream().filter(value -> !QuestManager.isCompleted(player, value.quest())).findFirst().orElse(null);
            if (current == null) {
                continue;
            }
            Quest quest = current.quest();
            if (!QuestManager.isStarted(player, quest)) {
                if (matches(player, quest.id() + "|start", current.start(), signal) && QuestManager.start(player, quest)) {
                    play(player, current.onStart());
                    enqueue(player, quest, current.startActions());
                }
                continue;
            }
            QuestStep active = QuestManager.currentStep(player, quest).orElse(null);
            if (active == null) {
                continue;
            }
            BoundStep step = current.steps().stream().filter(value -> value.id().equals(active.id())).findFirst().orElseThrow();
            if (matches(player, quest.id() + "|step|" + step.id(), step.trigger(), signal) && QuestManager.completeStep(player, quest, step.id())) {
                execution(player).complete(quest.id().toString(), step.id().toString());
                List<StoryDocument.Dialogue> dialogue = new ArrayList<>(step.dialogue());
                List<StoryDocument.Action> actions = new ArrayList<>(step.actions());
                if (QuestManager.isCompleted(player, quest)) {
                    dialogue.addAll(current.onComplete());
                    actions.addAll(current.completionActions());
                }
                play(player, dialogue);
                enqueue(player, quest, actions);
            }
        }
    }

    private static boolean matches(ServerPlayer player, String node, ResolvedTrigger trigger, TriggerSignal signal) {
        StoryExecutionState state = execution(player);
        if (signal == TriggerSignal.TICK) state.age(node);
        StoryDocument.Trigger definition = trigger.definition();
        if (!state.ready(node, definition.delay()) || definition.requires() != null && !eventCompleted(player, definition.requires())) return false;
        TriggerMemory memory = MEMORIES.computeIfAbsent(player.getUUID(), key -> new HashMap<>()).computeIfAbsent(node, key -> new TriggerMemory());
        if (!player.level().dimension().equals(trigger.dimension())) {
            memory.previous = null;
            return false;
        }
        if (!trigger.condition().matches(player, signal, memory)) return false;
        if (List.of("entity_killed", "item_picked_up", "item_crafted", "item_used", "block_broken").contains(definition.type())) {
            return state.increment(node, Math.max(0, signal.amount()), definition.count()) >= definition.count();
        }
        return true;
    }

    public static boolean eventCompleted(ServerPlayer player, String event) {
        return execution(player).completed(event) || quests.stream().anyMatch(quest -> quest.id().toString().equals(event) && QuestManager.isCompleted(player, quest));
    }

    private static StoryExecutionState execution(ServerPlayer player) {
        return player.getData(ModAttachments.EXECUTION);
    }

    private static void enqueue(ServerPlayer player, Quest quest, List<StoryDocument.Action> actions) {
        String owner = quest.id().toString();
        execution(player).enqueue(owner, actions, action -> executeAction(player, owner, action));
    }

    private static void executeAction(ServerPlayer player, String owner, StoryDocument.Action action) {
        if (action.type().equals("complete_event")) {
            execution(player).complete(owner, action.event());
        } else if (action.type().equals("dialogue")) {
            Component text = Component.literal(action.text()).withColor(Integer.parseInt(action.color().substring(1), 16));
            DialoguePayload.sendTo(player, text, action.duration(), false, true);
        }
    }

    public static boolean reset(ServerPlayer player, Quest quest) {
        forget(player);
        return execution(player).reset(quest.id().toString());
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
        return new ResolvedTrigger(dimension, trigger, TriggerConditions.resolve(server, trigger));
    }

    public static void forget(ServerPlayer player) {
        MEMORIES.remove(player.getUUID());
        com.thestone.storyapimod.event.StoryGameplayEvents.forget(player);
    }

    public static void clear() {
        com.thestone.storyapimod.event.StoryGameplayEvents.clear();
        chains = List.of();
        quests = List.of();
        catalog = QuestCatalogPayload.from(List.of());
        MEMORIES.clear();
    }

    private record Chain(List<BoundQuest> quests) {}

    private record BoundQuest(Quest quest, ResolvedTrigger start, List<BoundStep> steps, List<StoryDocument.Dialogue> onStart, List<StoryDocument.Dialogue> onComplete, List<StoryDocument.Action> startActions, List<StoryDocument.Action> completionActions) {}

    private record BoundStep(ResourceLocation id, ResolvedTrigger trigger, List<StoryDocument.Dialogue> dialogue, List<StoryDocument.Action> actions) {}

    private record ResolvedTrigger(ResourceKey<Level> dimension, StoryDocument.Trigger definition, TriggerCondition condition) {}
}
