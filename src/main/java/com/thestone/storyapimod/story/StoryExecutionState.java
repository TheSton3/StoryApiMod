package com.thestone.storyapimod.story;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class StoryExecutionState {
    private static final Gson GSON = new Gson();
    private final Map<String, Sequence> sequences = new HashMap<>();
    private final Map<String, Integer> counters = new HashMap<>();
    private final Map<String, Integer> ages = new HashMap<>();
    private final Map<String, Set<String>> events = new HashMap<>();

    public String save() {
        return GSON.toJson(this);
    }

    public static StoryExecutionState load(String json) {
        StoryExecutionState state = GSON.fromJson(json, StoryExecutionState.class);
        if (state == null) throw new IllegalArgumentException("Missing story execution state");
        return state;
    }

    public boolean busy(String owner) {
        return sequences.containsKey(owner);
    }

    public boolean completed(String event) {
        return events.values().stream().anyMatch(values -> values.contains(event));
    }

    public void complete(String owner, String event) {
        events.computeIfAbsent(owner, key -> new HashSet<>()).add(event);
    }

    public int increment(String node, int amount, int maximum) {
        int value = (int) Math.min(maximum, (long) counters.getOrDefault(node, 0) + amount);
        counters.put(node, value);
        return value;
    }

    public void age(String node) {
        ages.compute(node, (key, value) -> value == null ? 1 : Math.min(24000000, value + 1));
    }

    public boolean ready(String node, int delay) {
        return ages.getOrDefault(node, 0) >= delay;
    }

    public void enqueue(String owner, List<StoryDocument.Action> actions, Consumer<StoryDocument.Action> consumer) {
        if (actions.isEmpty()) return;
        Sequence sequence = sequences.computeIfAbsent(owner, key -> new Sequence());
        sequence.actions.addAll(actions);
        drain(owner, sequence, consumer);
    }

    public void tick(java.util.function.BiConsumer<String, StoryDocument.Action> consumer) {
        for (String owner : List.copyOf(sequences.keySet())) {
            Sequence sequence = sequences.get(owner);
            if (sequence.remaining > 0) sequence.remaining--;
            drain(owner, sequence, action -> consumer.accept(owner, action));
        }
    }

    private void drain(String owner, Sequence sequence, Consumer<StoryDocument.Action> consumer) {
        int budget = 64;
        while (sequence.remaining == 0 && sequence.index < sequence.actions.size() && budget-- > 0) {
            StoryDocument.Action action = sequence.actions.get(sequence.index++);
            if (action.type().equals("wait")) {
                sequence.remaining = action.ticks();
            } else {
                consumer.accept(action);
            }
        }
        if (sequence.remaining == 0 && sequence.index == sequence.actions.size()) sequences.remove(owner);
    }

    public boolean reset(String owner) {
        boolean changed = sequences.remove(owner) != null;
        changed |= events.remove(owner) != null;
        changed |= counters.keySet().removeIf(key -> key.startsWith(owner + "|"));
        changed |= ages.keySet().removeIf(key -> key.startsWith(owner + "|"));
        return changed;
    }

    public void retain(Set<String> owners) {
        Set<String> known = new HashSet<>(sequences.keySet());
        known.addAll(events.keySet());
        for (String node : counters.keySet()) known.add(node.split("\\|", 2)[0]);
        for (String node : ages.keySet()) known.add(node.split("\\|", 2)[0]);
        known.removeAll(owners);
        known.forEach(this::reset);
    }

    private static final class Sequence {
        private final List<StoryDocument.Action> actions = new ArrayList<>();
        private int index;
        private int remaining;
    }
}
