package com.thestone.storyapimod.story.triggers;

import com.thestone.storyapimod.story.StoryDocument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

public record LookAtCondition(StoryDocument.Trigger definition) implements TriggerCondition {
    public boolean matches(ServerPlayer player, TriggerSignal signal, TriggerMemory memory) {
        if (signal != TriggerSignal.TICK) return false;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(definition.distance()));
        BlockHitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        double nearestDistance = block.getType() == HitResult.Type.MISS ? definition.distance() * definition.distance() : eye.distanceToSqr(block.getLocation());
        Entity nearest = null;
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1), entity -> entity.isAlive() && entity.isPickable() && !entity.isSpectator())) {
            Optional<Vec3> intersection = entity.getBoundingBox().inflate(entity.getPickRadius()).clip(eye, end);
            double distance = entity.getBoundingBox().contains(eye) ? 0 : intersection.map(eye::distanceToSqr).orElse(Double.MAX_VALUE);
            if (distance < nearestDistance) {
                nearest = entity;
                nearestDistance = distance;
            }
        }
        if (definition.entity() != null) return TriggerConditions.entityMatches(definition, nearest);
        return nearest == null && block.getType() == HitResult.Type.BLOCK && BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(block.getBlockPos()).getBlock()).toString().equals(definition.block());
    }
}
