package com.thestone.storyapimod.core;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.jarjar.nio.util.Lazy;
import org.lwjgl.glfw.GLFW;

public final class ModKeymappings {
    public static final KeyMapping KEY_PRESS_OPEN_JOURNAL = new KeyMapping(
            "key.story.journal",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.story");


    public static final Lazy<KeyMapping> PRESS_OPEN_JOURNAL = Lazy.of(() -> KEY_PRESS_OPEN_JOURNAL);
}
