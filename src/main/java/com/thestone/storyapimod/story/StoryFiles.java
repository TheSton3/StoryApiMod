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
        return List.copyOf(documents);
    }
}
