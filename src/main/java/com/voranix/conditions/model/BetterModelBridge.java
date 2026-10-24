package com.voranix.conditions.model;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.EntityTrackerRegistry;
import org.bukkit.entity.Entity;

public final class BetterModelBridge implements ModelBridge {

    @Override
    public boolean apply(Entity entity, String model) {
        return BetterModel.model(model)
                .map(renderer -> renderer.getOrCreate(BukkitAdapter.adapt(entity)) != null)
                .orElse(false);
    }

    @Override
    public void animate(Entity entity, String animation) {
        EntityTrackerRegistry registry = BetterModel.registryOrNull(entity.getUniqueId());
        if (registry == null) return;
        for (EntityTracker tracker : registry.trackers()) tracker.animate(animation);
    }

    @Override
    public boolean has(Entity entity) {
        EntityTrackerRegistry registry = BetterModel.registryOrNull(entity.getUniqueId());
        return registry != null && !registry.trackers().isEmpty();
    }
}
