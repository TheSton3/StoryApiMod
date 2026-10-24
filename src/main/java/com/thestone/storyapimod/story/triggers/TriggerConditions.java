package com.thestone.storyapimod.story.triggers;

import com.thestone.storyapimod.story.StoryDocument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public final class TriggerConditions {
    private TriggerConditions() {}

    public static TriggerCondition resolve(MinecraftServer server, StoryDocument.Trigger definition) {
        if (definition.block() != null && !BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(definition.block()))) throw new IllegalArgumentException("Unknown block: " + definition.block());
        if (definition.entity() != null && !BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(definition.entity()))) throw new IllegalArgumentException("Unknown entity: " + definition.entity());
        if (definition.item() != null && (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(definition.item())) || definition.item().equals("minecraft:air"))) throw new IllegalArgumentException("Unknown or empty item: " + definition.item());
        if (definition.from() != null && server.levelKeys().stream().noneMatch(key -> key.location().toString().equals(definition.from()))) throw new IllegalArgumentException("Unknown source dimension: " + definition.from());
        return switch (definition.type()) {
            case "block_click" -> (player, signal, memory) -> signal.type().equals("block_click") && signal.block() != null && BuiltInRegistries.BLOCK.getKey(signal.block().getBlock()).toString().equals(definition.block());
            case "entity_click" -> (player, signal, memory) -> signal.type().equals("entity_click") && entityMatches(definition, signal.entity());
            case "area_enter" -> {
                AABB bounds = bounds(definition);
                yield (player, signal, memory) -> {
                    if (signal != TriggerSignal.TICK) return false;
                    boolean inside = bounds.contains(player.position());
                    boolean entered = !Boolean.TRUE.equals(memory.previous) && inside;
                    memory.previous = inside;
                    return entered;
                };
            }
            case "area_exit" -> new AreaExitCondition(bounds(definition));
            case "entity_killed" -> new EntityKilledCondition(definition);
            case "block_broken" -> new BlockBrokenCondition(definition);
            case "item_picked_up" -> new ItemPickedUpCondition(definition);
            case "item_crafted" -> new ItemCraftedCondition(definition);
            case "item_used" -> new ItemUsedCondition(definition);
            case "inventory_contains" -> new InventoryContainsCondition(definition);
            case "health_below" -> new HealthBelowCondition(definition);
            case "dimension_changed" -> new DimensionChangedCondition(definition);
            case "night_started" -> new NightStartedCondition();
            case "sleep_started" -> new SleepStartedCondition();
            case "sleep_ended" -> new SleepEndedCondition();
            case "look_at" -> new LookAtCondition(definition);
            case "npc_distance" -> new NpcDistanceCondition(definition);
            case "event_completed" -> new EventCompletedCondition(definition);
            default -> throw new IllegalArgumentException("Unknown trigger: " + definition.type());
        };
    }

    public static boolean entityMatches(StoryDocument.Trigger definition, Entity entity) {
        return entity != null && BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(definition.entity()) && (definition.tag() == null || entity.getTags().contains(definition.tag()));
    }

    public static boolean itemMatches(StoryDocument.Trigger definition, ItemStack stack) {
        return stack != null && !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(definition.item());
    }

    private static AABB bounds(StoryDocument.Trigger definition) {
        return new AABB(definition.min().get(0), definition.min().get(1), definition.min().get(2), definition.max().get(0), definition.max().get(1), definition.max().get(2));
    }
}
