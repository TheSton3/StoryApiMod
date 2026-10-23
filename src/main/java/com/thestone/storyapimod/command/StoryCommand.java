package com.thestone.storyapimod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import com.thestone.storyapimod.story.StoryRuntime;
import java.io.IOException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class StoryCommand {
    private StoryCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("story").requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload").executes(context -> reload(context.getSource()))));
    }

    private static int reload(CommandSourceStack source) {
        try {
            int count = StoryRuntime.reload(source.getServer());
            source.sendSuccess(() -> Component.literal("Сюжет загружен. Квестов: " + count), false);
            return count;
        } catch (IOException exception) {
            LogUtils.getLogger().error("Story reload rejected; previous stories are retained", exception);
            source.sendFailure(Component.literal("Ошибка сюжета: " + exception.getMessage() + ". Предыдущий сюжет сохранён."));
            return 0;
        }
    }
}
