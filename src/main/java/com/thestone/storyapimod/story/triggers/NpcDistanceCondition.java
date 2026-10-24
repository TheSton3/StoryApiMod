package com.thestone.storyapimod.story.triggers;

import com.thestone.storyapimod.story.StoryDocument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public record NpcDistanceCondition(StoryDocument.Trigger definition) implements TriggerCondition {
    public boolean matches(ServerPlayer player, TriggerSignal signal, TriggerMemory memory) {
        if (signal != TriggerSignal.TICK) return false;
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(definition.distance()), entity -> TriggerConditions.entityMatches(definition, entity))) {
            if (entity.isAlive() && player.distanceToSqr(entity) <= definition.distance() * definition.distance()) return true;
        }
        return false;
    }
}
