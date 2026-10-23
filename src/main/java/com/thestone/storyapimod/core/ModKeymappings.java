package com.thestone.storyapimod.core;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.jarjar.nio.util.Lazy;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ModKeymappings {
    public static final Lazy<KeyMapping> PRESS_OPEN_JOURNAL = Lazy.of(() -> new KeyMapping(
            "key.story.journal", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.story"));

    private ModKeymappings() {}
}
