package com.voranix.conditions.script;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;

/** Busca efectos, entidades y partículas por nombre ("poison", "minecraft:zombie", "LARGE_SMOKE"). */
public final class Registries {

    private Registries() {
    }

    public static PotionEffectType effect(String name) {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT).get(key(name));
    }

    public static EntityType entity(String name) {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENTITY_TYPE).get(key(name));
    }

    public static Particle particle(String name) {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.PARTICLE_TYPE).get(key(name));
    }

    private static NamespacedKey key(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        NamespacedKey k = NamespacedKey.fromString(n);
        return k != null ? k : NamespacedKey.minecraft(n);
    }
}
