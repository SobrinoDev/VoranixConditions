package com.voranix.conditions.mob;

import com.voranix.conditions.VoranixConditions;
import com.voranix.conditions.script.Context;
import com.voranix.conditions.script.Registries;
import com.voranix.conditions.script.Trigger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Carga los mobs de plugins/VoranixConditions/mobs, los spawnea y ejecuta sus comportamientos.
 * Cada mob queda marcado con su id en el PersistentDataContainer, así sobrevive reinicios.
 */
public final class MobManager {

    public static final NamespacedKey KEY = new NamespacedKey("voranixconditions", "mob");
    private static final String[] DEFAULTS = {"mobs/golem_oscuro.yml"};

    private final VoranixConditions plugin;
    private final File folder;
    private final Map<String, CustomMob> mobs = new LinkedHashMap<>();
    private final Map<UUID, LivingEntity> live = new HashMap<>();

    public MobManager(VoranixConditions plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "mobs");
    }

    public static String idOf(Entity entity) {
        return entity.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
    }

    public void load() {
        if (!folder.exists()) {
            for (String res : DEFAULTS) plugin.saveResource(res, false);
        }
        mobs.clear();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files != null) {
            for (File file : files) {
                try {
                    CustomMob mob = parse(YamlConfiguration.loadConfiguration(file), file);
                    mobs.put(mob.id().toLowerCase(Locale.ROOT), mob);
                } catch (Exception e) {
                    plugin.getLogger().warning("No se pudo cargar " + file.getName() + ": " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info(mobs.size() + " mobs personalizados cargados.");
    }

    private CustomMob parse(YamlConfiguration yml, File file) {
        String id = yml.getString("id", file.getName().replaceFirst("\\.ya?ml$", ""));
        EntityType base = Registries.entity(yml.getString("base", "zombie"));
        if (base == null || base.getEntityClass() == null || !LivingEntity.class.isAssignableFrom(base.getEntityClass())) {
            throw new IllegalArgumentException("'base' debe ser una entidad viva válida");
        }
        Map<String, String> animations = new HashMap<>();
        ConfigurationSection anim = yml.getConfigurationSection("animations");
        if (anim != null) {
            for (String k : anim.getKeys(false)) animations.put(k.toLowerCase(Locale.ROOT), anim.getString(k));
        }
        EntityType replace = null;
        String replaceName = yml.getString("natural-spawn.replace");
        if (replaceName != null) {
            replace = Registries.entity(replaceName);
            if (replace == null) plugin.getLogger().warning(file.getName() + ": entidad '" + replaceName + "' no existe");
        }
        return new CustomMob(id, base, yml.getString("model"), yml.getString("name"), yml.getBoolean("name-visible", true),
                number(yml, "health"), number(yml, "damage"), number(yml, "speed"), number(yml, "scale"),
                number(yml, "armor"), number(yml, "knockback-resistance"), number(yml, "follow-range"),
                yml.getBoolean("silent", false), yml.getBoolean("drops-vanilla", false), yml.getString("script"),
                animations, Trigger.fromList(yml.getMapList("behaviors"), file.getName(), plugin.getLogger()),
                replace, yml.getDouble("natural-spawn.chance", 0), yml.getStringList("natural-spawn.worlds"));
    }

    private static Double number(YamlConfiguration yml, String path) {
        return yml.isSet(path) ? yml.getDouble(path) : null;
    }

    public Collection<CustomMob> all() {
        return mobs.values();
    }

    public CustomMob get(String id) {
        return id == null ? null : mobs.get(id.toLowerCase(Locale.ROOT));
    }

    public CustomMob of(Entity entity) {
        return get(idOf(entity));
    }

    public Collection<LivingEntity> live() {
        return live.values();
    }

    // ------------------------------------------------------------------ spawn

    public LivingEntity spawn(String id, Location location) {
        CustomMob mob = get(id);
        if (mob == null) return null;
        Entity spawned = location.getWorld().spawnEntity(location, mob.base(), CreatureSpawnEvent.SpawnReason.CUSTOM,
                e -> configure(mob, (LivingEntity) e));
        LivingEntity living = (LivingEntity) spawned;
        if (!living.isValid()) return null;
        track(living);
        fire(living, "spawn", new Context());
        return living;
    }

    private void configure(CustomMob mob, LivingEntity e) {
        e.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, mob.id());
        if (mob.name() != null) {
            e.customName(MiniMessage.miniMessage().deserialize(mob.name()));
            e.setCustomNameVisible(mob.nameVisible());
        }
        e.setSilent(mob.silent());
        e.setRemoveWhenFarAway(false);
        e.setPersistent(true);
        attribute(e, Attribute.MAX_HEALTH, mob.health());
        attribute(e, Attribute.ATTACK_DAMAGE, mob.damage());
        attribute(e, Attribute.MOVEMENT_SPEED, mob.speed());
        attribute(e, Attribute.SCALE, mob.scale());
        attribute(e, Attribute.ARMOR, mob.armor());
        attribute(e, Attribute.KNOCKBACK_RESISTANCE, mob.knockbackResistance());
        attribute(e, Attribute.FOLLOW_RANGE, mob.followRange());
        if (mob.health() != null) e.setHealth(mob.health());
    }

    private static void attribute(LivingEntity e, Attribute attribute, Double value) {
        if (value == null) return;
        AttributeInstance inst = e.getAttribute(attribute);
        if (inst == null) {
            e.registerAttribute(attribute);
            inst = e.getAttribute(attribute);
        }
        if (inst != null) inst.setBaseValue(value);
    }

    /** Reemplaza un spawn natural por un mob personalizado si toca. Devuelve true si lo reemplazó. */
    public boolean tryReplace(LivingEntity spawned) {
        for (CustomMob mob : mobs.values()) {
            if (mob.replace() != spawned.getType() || mob.replaceChance() <= 0) continue;
            if (!mob.worlds().isEmpty() && !mob.worlds().contains(spawned.getWorld().getName())) continue;
            if (mob.script() != null && !plugin.scripts().isEnabled(mob.script())) continue;
            if (ThreadLocalRandom.current().nextDouble() >= mob.replaceChance()) continue;
            spawn(mob.id(), spawned.getLocation());
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ seguimiento

    public void track(LivingEntity e) {
        CustomMob mob = of(e);
        if (mob == null) return;
        live.put(e.getUniqueId(), e);
        if (mob.model() != null) plugin.models().apply(e, mob.model());
    }

    public void untrack(Entity e) {
        live.remove(e.getUniqueId());
    }

    // ------------------------------------------------------------------ comportamientos

    /**
     * Ejecuta los comportamientos "on: event" del mob. El rol "entity" siempre es el mob
     * y "target" su objetivo actual. Las animaciones del bloque animations: se reproducen siempre.
     */
    public void fire(Entity entity, String event, Context ctx) {
        CustomMob mob = of(entity);
        if (mob == null) return;
        String anim = mob.animations().get(event);
        if (anim != null) plugin.models().animate(entity, anim);
        if (mob.script() != null && !plugin.scripts().isEnabled(mob.script())) return;

        ctx.role("entity", entity);
        if (entity instanceof Mob m && ctx.role("target") == null) ctx.role("target", m.getTarget());
        for (Trigger t : mob.behaviors()) {
            if (t.event().equals(event)) plugin.actions().fire(mob.id(), t, ctx);
        }
    }

    /** Llamado cada tick: comportamientos "tick" y re-aplicar modelos que falten. */
    public void tick(long tick) {
        List<LivingEntity> snapshot = new ArrayList<>(live.values());
        for (LivingEntity e : snapshot) {
            if (!e.isValid()) {
                if (e.isDead()) live.remove(e.getUniqueId());
                continue;
            }
            CustomMob mob = of(e);
            if (mob == null) {
                live.remove(e.getUniqueId());
                continue;
            }
            // BetterModel carga sus modelos en async al iniciar; reintentamos por si aún no estaban
            if (tick % 100 == 0 && mob.model() != null && !plugin.models().has(e)) {
                plugin.models().apply(e, mob.model());
            }
            if (mob.script() != null && !plugin.scripts().isEnabled(mob.script())) continue;
            for (Trigger t : mob.behaviors()) {
                if (t.event().equals("tick") && tick % t.interval() == 0) {
                    Context ctx = new Context().role("entity", e);
                    if (e instanceof Mob m) ctx.role("target", m.getTarget());
                    plugin.actions().fire(mob.id(), t, ctx);
                }
            }
        }
    }
}
