package com.voranix.conditions.model;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.data.renderer.BlueprintRenderer;
import kr.toxicity.model.api.tracker.EntityTracker;
import org.bukkit.entity.Entity;

/** Puente para BetterModel 1.4.x (la última serie compatible con 1.20.1). */
public final class BetterModelBridge implements ModelBridge {

    @Override
    public boolean apply(Entity entity, String model) {
        if (EntityTracker.tracker(entity) != null) return true;
        BlueprintRenderer renderer = BetterModel.inst().modelManager().renderer(model);
        return renderer != null && renderer.create(entity) != null;
    }

    @Override
    public void animate(Entity entity, String animation) {
        EntityTracker tracker = EntityTracker.tracker(entity);
        if (tracker != null) tracker.animateSingle(animation);
    }

    @Override
    public boolean has(Entity entity) {
        return EntityTracker.tracker(entity) != null;
    }
}
