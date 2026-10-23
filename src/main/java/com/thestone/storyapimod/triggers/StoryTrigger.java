package com.thestone.storyapimod.triggers;

import com.thestone.storyapimod.util.StoryProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class StoryTrigger {
    private final ResourceLocation id;
    private final boolean once;

    protected StoryTrigger(ResourceLocation id, boolean once) {
        this.id = id;
        this.once = once;
    }

    public final ResourceLocation getId() {
        return id;
    }

    public final boolean isCompleted(ServerPlayer player) {
        return StoryProgress.isCompleted(player, id);
    }

    protected final void execute(ServerPlayer player, Runnable action) {
        if (once && !StoryProgress.complete(player, id)) {
            return;
        }

        action.run();
    }
}
