package com.thestone.storyapimod.story;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class StoryFilesTest {
    private static int checks;

    private StoryFilesTest() {}

    public static void main(String[] args) throws IOException {
        Path directory = Files.createTempDirectory("story-parser-test-");
        try {
            String example = Files.readString(Path.of("src/main/resources/story_examples/village.json"));
            Path file = directory.resolve("village.json");
            Files.writeString(file, example);
            List<StoryDocument> loaded = StoryFiles.read(directory);
            check(loaded.size() == 1, "One story must load");
            check(loaded.getFirst().quests().size() == 2, "Quest sequence must survive parsing");
            check(loaded.getFirst().quests().getFirst().steps().getFirst().trigger().dimension().equals("minecraft:overworld"), "Default dimension must be applied");
            check(loaded.getFirst().quests().getFirst().steps().get(2).dialogue().isEmpty(), "Missing optional dialogue must be empty");
            check(loaded.getFirst().quests().getFirst().reward().count() == 3, "Reward must survive parsing");
            check(loaded.getFirst().quests().getFirst().onStart().getFirst().text().contains("добро пожаловать"), "UTF-8 dialogue must survive parsing");

            String waiting = Files.readString(Path.of("src/main/resources/story_examples/wait_for_guide.json"));
            Files.writeString(file, waiting);
            List<StoryDocument> waitingStory = StoryFiles.read(directory);
            check(waitingStory.getFirst().quests().getFirst().completionActions().getFirst().ticks() == 24000, "Example must wait a full day");
            check(waitingStory.getFirst().quests().get(1).start().requires().equals("storyapimod:guide_ready"), "Next conversation must depend on the completed event");
            try {
                Files.writeString(file, waiting.replace("\"requires\": \"storyapimod:guide_ready\"", "\"requires\": \"storyapimod:missing\""));
                StoryFiles.read(directory);
                throw new AssertionError("Unknown event reference accepted");
            } catch (IOException exception) {
                check(exception.getMessage().contains("storyapimod:missing"), "Invalid references must identify the missing event");
            }

            reject(file, directory, example.replace("\"version\": 1", "\"version\": 2"));
            reject(file, directory, example.replace("[20, -50, 20]", "[20, -61, 20]"));
            reject(file, directory, example.replace("block_click", "unsupported"));
            reject(file, directory, example.replace("\"duration\": 120", "\"duration\": 0"));
            reject(file, directory, example.replace("storyapimod:return_to_guide", "storyapimod:find_chest"));
            reject(file, directory, example.replace("minecraft:chest", "INVALID ID"));
            reject(file, directory, "{broken json");
            reject(file, directory, "null");

            Files.writeString(file, example);
            Path duplicate = directory.resolve("duplicate.json");
            Files.writeString(duplicate, example.replace("storyapimod:village_story", "storyapimod:other_story"));
            expectFailure(directory);
            Files.delete(duplicate);
            Files.writeString(directory.resolve("ignored.txt"), "not json");
            check(StoryFiles.read(directory).size() == 1, "Non-JSON files must be ignored");
            Files.delete(file);
            check(StoryFiles.read(directory).isEmpty(), "An empty folder disables stories");
            System.out.println("Story parser checks passed: " + checks);
        } finally {
            try (Stream<Path> paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    private static void reject(Path file, Path directory, String json) throws IOException {
        Files.writeString(file, json);
        expectFailure(directory);
    }

    private static void expectFailure(Path directory) throws IOException {
        try {
            StoryFiles.read(directory);
        } catch (IOException exception) {
            check(exception.getMessage().contains(".json"), "Errors must name the source file");
            return;
        }
        throw new AssertionError("Invalid story was accepted");
    }

    private static void check(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
