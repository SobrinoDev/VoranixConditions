package com.voranix.conditions.script;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;
import java.util.Map;

/** Busca efectos, entidades y partículas por nombre ("poison", "minecraft:zombie", "large_smoke"). */
public final class Registries {

    /**
     * En 1.20.1 las partículas usan nombres antiguos de Bukkit (SMOKE_LARGE, REDSTONE...).
     * Así se pueden escribir con el nombre de Minecraft, igual que en versiones nuevas.
     */
    private static final Map<String, String> PARTICLE_ALIASES = Map.ofEntries(
            Map.entry("large_smoke", "SMOKE_LARGE"),
            Map.entry("smoke", "SMOKE_NORMAL"),
            Map.entry("explosion", "EXPLOSION_LARGE"),
            Map.entry("explosion_emitter", "EXPLOSION_HUGE"),
            Map.entry("poof", "EXPLOSION_NORMAL"),
            Map.entry("dust", "REDSTONE"),
            Map.entry("effect", "SPELL"),
            Map.entry("entity_effect", "SPELL_MOB"),
            Map.entry("instant_effect", "SPELL_INSTANT"),
            Map.entry("witch", "SPELL_WITCH"),
            Map.entry("enchant", "ENCHANTMENT_TABLE"),
            Map.entry("enchanted_hit", "CRIT_MAGIC"),
            Map.entry("happy_villager", "VILLAGER_HAPPY"),
            Map.entry("angry_villager", "VILLAGER_ANGRY"),
            Map.entry("splash", "WATER_SPLASH"),
            Map.entry("bubble", "WATER_BUBBLE"),
            Map.entry("rain", "WATER_DROP"),
            Map.entry("fishing", "WATER_WAKE"),
            Map.entry("item_slime", "SLIME"),
            Map.entry("item_snowball", "SNOWBALL"),
            Map.entry("mycelium", "TOWN_AURA"),
            Map.entry("firework", "FIREWORKS_SPARK"),
            Map.entry("totem_of_undying", "TOTEM"),
            Map.entry("dripping_lava", "DRIP_LAVA"),
            Map.entry("dripping_water", "DRIP_WATER"),
            Map.entry("elder_guardian", "MOB_APPEARANCE"),
            Map.entry("underwater", "SUSPENDED_DEPTH"),
            Map.entry("item", "ITEM_CRACK"),
            Map.entry("block", "BLOCK_CRACK")
    );

    private Registries() {
    }

    public static PotionEffectType effect(String name) {
        PotionEffectType type = PotionEffectType.getByKey(key(name));
        return type != null ? type : PotionEffectType.getByName(name.toUpperCase(Locale.ROOT));
    }

    public static EntityType entity(String name) {
        return Registry.ENTITY_TYPE.get(key(name));
    }

    public static Particle particle(String name) {
        String n = key(name).getKey();
        String upper = PARTICLE_ALIASES.getOrDefault(n, n.toUpperCase(Locale.ROOT));
        try {
            return Particle.valueOf(upper);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static NamespacedKey key(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        NamespacedKey k = NamespacedKey.fromString(n);
        return k != null ? k : NamespacedKey.minecraft(n);
    }
}
