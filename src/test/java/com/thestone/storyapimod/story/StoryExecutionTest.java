package com.thestone.storyapimod.story;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class StoryExecutionTest {
    private static final Gson GSON = new Gson();
    private static int checks;

    public static void main(String[] args) {
        StoryExecutionState first = new StoryExecutionState();
        StoryExecutionState second = new StoryExecutionState();
        List<String> played = new ArrayList<>();
        List<StoryDocument.Action> actions = List.of(action("{\"type\":\"dialogue\",\"text\":\"Before\"}"), action("{\"type\":\"WAIT\",\"ticks\":24000}"), action("{\"type\":\"complete_event\",\"event\":\"test:ready\"}"), action("{\"type\":\"dialogue\",\"text\":\"After\"}"));
        first.enqueue("test:quest", actions, value -> played.add(value.text()));
        check(first.busy("test:quest") && played.equals(List.of("Before")), "WAIT must block later actions");
        check(!second.busy("test:quest") && !second.completed("test:ready"), "Players must be independent");
        for (int tick = 0; tick < 12000; tick++) first.tick((owner, value) -> { throw new AssertionError("Early action"); });
        first = StoryExecutionState.load(first.save());
        check(first.busy("test:quest"), "Reload must keep the remaining WAIT");
        for (int tick = 0; tick < 11999; tick++) first.tick((owner, value) -> { throw new AssertionError("Early action after reload"); });
        StoryExecutionState resumed = first;
        first.tick((owner, value) -> {
            if (value.type().equals("complete_event")) resumed.complete(owner, value.event());
            else played.add(value.text());
        });
        check(!first.busy("test:quest") && first.completed("test:ready"), "WAIT must finish at tick 24000");
        check(played.equals(List.of("Before", "After")), "Action ordering must survive save/load");
        first.enqueue("test:quest", List.of(action("{\"type\":\"wait\",\"ticks\":3}")), value -> {});
        check(first.increment("test:quest|step|test:collect", 3, 5) == 3, "Partial progress must accumulate");
        first.age("test:quest|start");
        first = StoryExecutionState.load(first.save());
        check(first.increment("test:quest|step|test:collect", 4, 5) == 5, "Counts must persist and cap at target");
        check(first.ready("test:quest|start", 1) && !first.ready("test:quest|start", 2), "Delay progress must persist");
        check(first.reset("test:quest"), "Reset must report state changes");
        check(!first.busy("test:quest") && !first.completed("test:ready") && !first.ready("test:quest|start", 1), "Reset must clear waits, events and delay");
        first.tick((owner, value) -> { throw new AssertionError("Reset task executed"); });
        check(first.increment("test:quest|step|test:collect", 1, 5) == 1, "Reset must clear counts");
        first.retain(Set.of());
        check(first.increment("test:quest|step|test:collect", 1, 5) == 1, "Removed quests must lose their counters");
        StoryExecutionState multiple = new StoryExecutionState();
        multiple.enqueue("test:quest", List.of(action("{\"type\":\"wait\",\"ticks\":1}"), action("{\"type\":\"wait\",\"ticks\":2}"), action("{\"type\":\"complete_event\",\"event\":\"test:done\"}")), value -> {});
        multiple.tick((owner, value) -> { throw new AssertionError("First WAIT skipped"); });
        multiple.tick((owner, value) -> { throw new AssertionError("Second WAIT skipped"); });
        multiple.tick((owner, value) -> multiple.complete(owner, value.event()));
        check(multiple.completed("test:done") && !multiple.busy("test:quest"), "Consecutive WAITs must add up");
        rejectAction("{\"type\":\"wait\",\"ticks\":0}");
        rejectAction("{\"type\":\"wait\",\"ticks\":-5}");
        rejectAction("{\"type\":\"command\",\"text\":\"op Player\"}");
        rejectTrigger("{\"type\":\"health_below\",\"health\":0}");
        rejectTrigger("{\"type\":\"item_crafted\",\"item\":\"minecraft:stick\",\"count\":0}");
        rejectTrigger("{\"type\":\"look_at\",\"block\":\"minecraft:stone\",\"entity\":\"minecraft:pig\"}");
        rejectTrigger("{\"type\":\"npc_distance\",\"entity\":\"minecraft:villager\",\"distance\":-1}");
        rejectTrigger("{\"type\":\"item_used\",\"item\":\"minecraft:apple\",\"phase\":\"bad\"}");
        rejectTrigger("{\"type\":\"block_click\",\"block\":\"minecraft:stone\",\"delay\":-1}");
        String[] triggers = {
            "{\"type\":\"entity_killed\",\"entity\":\"minecraft:zombie\",\"count\":3}",
            "{\"type\":\"item_picked_up\",\"item\":\"minecraft:apple\",\"count\":5}",
            "{\"type\":\"item_crafted\",\"item\":\"minecraft:stick\"}",
            "{\"type\":\"block_broken\",\"block\":\"minecraft:stone\"}",
            "{\"type\":\"item_used\",\"item\":\"minecraft:apple\"}",
            "{\"type\":\"dimension_changed\",\"dimension\":\"minecraft:the_nether\",\"from\":\"minecraft:overworld\"}",
            "{\"type\":\"night_started\"}",
            "{\"type\":\"inventory_contains\",\"item\":\"minecraft:emerald\",\"count\":3}",
            "{\"type\":\"health_below\",\"health\":6}",
            "{\"type\":\"sleep_started\"}",
            "{\"type\":\"sleep_ended\"}",
            "{\"type\":\"area_exit\",\"min\":[0,0,0],\"max\":[5,5,5]}",
            "{\"type\":\"look_at\",\"entity\":\"minecraft:villager\",\"tag\":\"guide\"}",
            "{\"type\":\"npc_distance\",\"entity\":\"minecraft:villager\",\"distance\":4}",
            "{\"type\":\"event_completed\",\"event\":\"test:ready\"}"
        };
        for (String json : triggers) check(GSON.fromJson(json, StoryDocument.Trigger.class) != null, "New trigger must parse");
        System.out.println("Story execution checks passed: " + checks);
    }

    private static StoryDocument.Action action(String json) {
        return GSON.fromJson(json, StoryDocument.Action.class);
    }

    private static void rejectAction(String json) {
        try {
            action(json);
        } catch (RuntimeException exception) {
            checks++;
            return;
        }
        throw new AssertionError("Invalid action accepted: " + json);
    }

    private static void rejectTrigger(String json) {
        try {
            GSON.fromJson(json, StoryDocument.Trigger.class);
        } catch (RuntimeException exception) {
            checks++;
            return;
        }
        throw new AssertionError("Invalid trigger accepted: " + json);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
