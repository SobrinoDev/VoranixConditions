package com.voranix.conditions.model;

import org.bukkit.entity.Entity;

/**
 * Acceso a BetterModel. Si el plugin no está instalado se usa {@link #NONE}
 * y las clases de BetterModel nunca se cargan.
 */
public interface ModelBridge {

    ModelBridge NONE = new ModelBridge() {
        @Override
        public boolean apply(Entity entity, String model) {
            return false;
        }

        @Override
        public void animate(Entity entity, String animation) {
        }

        @Override
        public boolean has(Entity entity) {
            return false;
        }
    };

    /** Pone el modelo sobre la entidad (no hace nada si ya lo tiene). */
    boolean apply(Entity entity, String model);

    void animate(Entity entity, String animation);

    boolean has(Entity entity);
}
