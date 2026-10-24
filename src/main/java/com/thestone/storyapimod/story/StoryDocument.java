package com.thestone.storyapimod.story;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record StoryDocument(int version, String id, List<QuestDefinition> quests) {
    public StoryDocument {
        require(version == 1, "version must be 1");
        id = identifier(id);
        quests = List.copyOf(Objects.requireNonNull(quests, "quests"));
        require(!quests.isEmpty(), "quests must not be empty");
        Set<String> ids = new HashSet<>();
        for (QuestDefinition quest : quests) {
            require(ids.add(quest.id()), "Duplicate quest: " + quest.id());
        }
    }

    public record QuestDefinition(String id, String title, Trigger start, List<Dialogue> onStart, List<StepDefinition> steps, List<Dialogue> onComplete, Reward reward, List<Action> startActions, List<Action> completionActions) {
        public QuestDefinition {
            id = identifier(id);
            title = text(title, "title");
            Objects.requireNonNull(start, "start");
            onStart = optional(onStart);
            onComplete = optional(onComplete);
            startActions = optional(startActions);
            completionActions = optional(completionActions);
            steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
            require(!steps.isEmpty(), "steps must not be empty");
            Set<String> ids = new HashSet<>();
            for (StepDefinition step : steps) {
                require(ids.add(step.id()), "Duplicate step: " + step.id());
            }
        }
    }

    public record StepDefinition(String id, String description, Trigger trigger, List<Dialogue> dialogue, List<Action> actions) {
        public StepDefinition {
            id = identifier(id);
            description = text(description, "description");
            Objects.requireNonNull(trigger, "trigger");
            dialogue = optional(dialogue);
            actions = optional(actions);
        }
    }

    public record Trigger(String type, String block, String entity, String tag, String dimension, List<Double> min, List<Double> max, String item, Integer count, Double health, Double distance, String from, String event, String requires, Integer delay, String phase) {
        public Trigger {
            type = text(type, "trigger.type");
            dimension = identifier(dimension == null ? "minecraft:overworld" : dimension);
            count = count == null ? 1 : count;
            delay = delay == null ? 0 : delay;
            distance = distance == null ? 6.0 : distance;
            require(count > 0 && count <= 1000000, "trigger.count must be 1..1000000");
            require(delay >= 0 && delay <= 24000000, "trigger.delay must be 0..24000000 ticks");
            require(Double.isFinite(distance) && distance > 0 && distance <= 128, "trigger.distance must be 0..128");
            if (requires != null) requires = identifier(requires);
            if (from != null) from = identifier(from);
            if (tag != null) tag = text(tag, "tag");
            switch (type) {
                case "block_click", "block_broken" -> block = identifier(block);
                case "entity_click", "entity_killed", "npc_distance" -> {
                    entity = identifier(entity);
                    if (tag != null) {
                        tag = text(tag, "tag");
                    }
                }
                case "area_enter", "area_exit" -> {
                    min = coordinates(min);
                    max = coordinates(max);
                    for (int axis = 0; axis < 3; axis++) {
                        require(min.get(axis) < max.get(axis), "area min must be less than max on every axis");
                    }
                }
                case "item_picked_up", "item_crafted", "item_used", "inventory_contains" -> {
                    item = identifier(item);
                    phase = phase == null ? "finish" : phase;
                    require(List.of("start", "finish", "release", "click").contains(phase), "Invalid item use phase");
                }
                case "health_below" -> require(health != null && Double.isFinite(health) && health > 0, "trigger.health must be positive");
                case "look_at" -> {
                    require((block == null) != (entity == null), "look_at requires exactly one of block or entity");
                    if (block != null) block = identifier(block);
                    if (entity != null) entity = identifier(entity);
                }
                case "event_completed" -> event = identifier(event);
                case "dimension_changed", "night_started", "sleep_started", "sleep_ended" -> {}
                default -> throw new IllegalArgumentException("Unknown trigger type: " + type);
            }
        }
    }

    public record Action(String type, Integer ticks, String text, Integer duration, String color, String event) {
        public Action {
            type = StoryDocument.text(type, "action.type").toLowerCase(java.util.Locale.ROOT);
            switch (type) {
                case "wait" -> require(ticks != null && ticks > 0 && ticks <= 24000000, "wait.ticks must be 1..24000000");
                case "dialogue" -> {
                    duration = duration == null ? 100 : duration;
                    Dialogue validated = new Dialogue(text, duration, color);
                    text = validated.text();
                    color = validated.color();
                }
                case "complete_event" -> event = identifier(event);
                default -> throw new IllegalArgumentException("Unknown action type: " + type);
            }
        }
    }

    public record Dialogue(String text, int duration, String color) {
        public Dialogue {
            text = StoryDocument.text(text, "dialogue.text");
            require(duration >= 30 && duration <= 12000, "dialogue.duration must be 30..12000 ticks");
            color = color == null ? "#FFFFFF" : color;
            require(color.matches("#[0-9a-fA-F]{6}"), "dialogue.color must be #RRGGBB");
        }
    }

    public record Reward(String item, int count) {
        public Reward {
            item = identifier(item);
            require(count >= 1 && count <= 2304, "reward.count must be 1..2304");
        }
    }

    private static String identifier(String value) {
        require(value != null && value.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"), "Invalid namespaced ID: " + value);
        return value;
    }

    private static String text(String value, String field) {
        require(value != null && !value.isBlank() && value.length() <= 2048, field + " must contain 1..2048 characters");
        return value;
    }

    private static List<Double> coordinates(List<Double> value) {
        require(value != null && value.size() == 3, "area coordinates must contain three numbers");
        for (Double coordinate : value) {
            require(coordinate != null && Double.isFinite(coordinate), "Coordinates must be finite");
        }
        return List.copyOf(value);
    }

    private static <T> List<T> optional(List<T> value) {
        return value == null ? List.of() : List.copyOf(value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
