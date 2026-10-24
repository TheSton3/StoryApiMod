package com.thestone.storyapimod.story;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class StoryFiles {
    private static final Gson GSON = new Gson();

    private StoryFiles() {}

    public static List<StoryDocument> read(Path directory) throws IOException {
        Files.createDirectories(directory);
        List<Path> paths;
        try (Stream<Path> stream = Files.list(directory)) {
            paths = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted().toList();
        }
        List<StoryDocument> documents = new ArrayList<>();
        Set<String> stories = new HashSet<>();
        Set<String> quests = new HashSet<>();
        for (Path path : paths) {
            try {
                if (Files.size(path) > 1048576) {
                    throw new IllegalArgumentException("File exceeds 1 MiB");
                }
                StoryDocument document;
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    document = GSON.fromJson(reader, StoryDocument.class);
                }
                if (document == null || !stories.add(document.id())) {
                    throw new IllegalArgumentException("Empty document or duplicate story ID");
                }
                for (StoryDocument.QuestDefinition quest : document.quests()) {
                    if (!quests.add(quest.id())) {
                        throw new IllegalArgumentException("Duplicate quest ID across files: " + quest.id());
                    }
                }
                documents.add(document);
            } catch (RuntimeException exception) {
                throw new IOException(path.getFileName() + ": " + exception.getMessage(), exception);
            }
        }
        validateReferences(documents);
        return List.copyOf(documents);
    }

    private static void validateReferences(List<StoryDocument> documents) throws IOException {
        Set<String> events = new HashSet<>();
        List<StoryDocument.Trigger> triggers = new ArrayList<>();
        for (StoryDocument document : documents) {
            for (StoryDocument.QuestDefinition quest : document.quests()) {
                events.add(quest.id());
                triggers.add(quest.start());
                collectEvents(events, quest.startActions());
                collectEvents(events, quest.completionActions());
                for (StoryDocument.StepDefinition step : quest.steps()) {
                    events.add(step.id());
                    triggers.add(step.trigger());
                    collectEvents(events, step.actions());
                }
            }
        }
        for (StoryDocument.Trigger trigger : triggers) {
            if (trigger.requires() != null && !events.contains(trigger.requires())) throw new IOException("Unknown event reference in story JSON: " + trigger.requires());
            if (trigger.type().equals("event_completed") && !events.contains(trigger.event())) throw new IOException("Unknown event reference in story JSON: " + trigger.event());
        }
    }

    private static void collectEvents(Set<String> events, List<StoryDocument.Action> actions) {
        for (StoryDocument.Action action : actions) {
            if (action.type().equals("complete_event")) events.add(action.event());
        }
    }
}
