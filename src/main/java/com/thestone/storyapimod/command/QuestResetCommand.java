package com.thestone.storyapimod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.thestone.storyapimod.story.StoryRuntime;
import com.thestone.storyapimod.quest.Quest;
import com.thestone.storyapimod.quest.QuestManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class QuestResetCommand {
    private static final DynamicCommandExceptionType UNKNOWN_QUEST =
            new DynamicCommandExceptionType(id ->
                    Component.literal("Неизвестный квест: " + id)
            );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("quest")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("reset")
                                        .then(
                                                Commands.argument("quest", ResourceLocationArgument.id())
                                                        .suggests((context, builder) ->
                                                                SharedSuggestionProvider.suggestResource(
                                                                        StoryRuntime.quests().stream().map(Quest::id),
                                                                        builder
                                                                )
                                                        )
                                                        .executes(context ->
                                                                reset(context, context.getSource().getPlayerOrException())
                                                        )
                                                        .then(
                                                                Commands.argument("player", EntityArgument.player())
                                                                        .executes(context ->
                                                                                reset(context, EntityArgument.getPlayer(context, "player"))
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int reset(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(context, "quest");

        Quest quest = StoryRuntime.quests().stream()
                .filter(value -> value.id().equals(id))
                .findFirst()
                .orElseThrow(() -> UNKNOWN_QUEST.create(id));

        boolean changed = QuestManager.reset(player, quest);

        if (!changed) {
            context.getSource().sendSuccess(
                    () -> Component.literal("У игрока ")
                            .append(player.getDisplayName())
                            .append(" нет прогресса квеста " + id + "."),
                    false
            );

            return 0;
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Квест ")
                        .append(quest.title())
                        .append(" сброшен для игрока ")
                        .append(player.getDisplayName())
                        .append(". Его можно взять заново."),
                false
        );

        return 1;
    }
}
