package com.voranix.conditions.script;

import com.voranix.conditions.VoranixConditions;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ejecuta las acciones de un trigger (ver {@link Action} para los formatos aceptados).
 */
public final class ActionRunner {

    private static final int MAX_DEPTH = 4;

    private final VoranixConditions plugin;
    private int depth;
    private boolean debug;

    public ActionRunner(VoranixConditions plugin) {
        this.plugin = plugin;
    }

    public boolean debug() {
        return debug;
    }

    public void debug(boolean debug) {
        this.debug = debug;
    }

    /** Revisa las condiciones y, si se cumplen todas, ejecuta las acciones. "source" es el script o mob dueño. */
    public boolean fire(String source, Trigger trigger, Context ctx) {
        // Evita bucles infinitos, p. ej. una acción "damage" dentro de un trigger "damage"
        if (depth >= MAX_DEPTH) return false;
        depth++;
        try {
            for (String condition : trigger.conditions()) {
                if (!Expression.test(condition, ctx)) {
                    // Los timers corren cada pocos ticks: no se registran para no llenar la consola
                    if (debug && !trigger.event().equals("timer") && !trigger.event().equals("tick")) {
                        plugin.getLogger().info("[debug] " + source + " (" + trigger.event() + ") no se cumplió: "
                                + condition + "  →  " + Expression.resolve(condition, ctx));
                    }
                    return false;
                }
            }
            if (debug) plugin.getLogger().info("[debug] " + source + " (" + trigger.event() + ") ejecutado");
            run(trigger.actions(), 0, ctx);
            return true;
        } finally {
            depth--;
        }
    }

    public void run(List<Action> actions, int start, Context ctx) {
        for (int i = start; i < actions.size(); i++) {
            Action action = actions.get(i);
            Args a = new Args(action, ctx);
            try {
                if (action.name().equals("delay")) {
                    long ticks = Math.max(1, a.ticks(0, 20, "duration", "time", "value"));
                    int next = i + 1;
                    Context later = ctx.detached();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> run(actions, next, later), ticks);
                    return;
                }
                execute(action.name(), a, ctx);
            } catch (Exception e) {
                plugin.getLogger().warning("Acción inválida '" + action.raw() + "': " + e.getMessage());
            }
        }
    }

    private void execute(String name, Args a, Context ctx) {
        switch (name) {
            // ---- Modificar el evento actual ----
            case "cancel" -> {
                if (ctx.event() instanceof Cancellable c) c.setCancelled(true);
            }
            case "damage_multiply" -> {
                double x = a.num(0, "value", "amount");
                damageEvent(ctx, d -> d * x);
            }
            case "damage_add" -> {
                double x = a.num(0, "value", "amount");
                damageEvent(ctx, d -> d + x);
            }
            case "damage_set" -> {
                double x = a.num(0, "value", "amount");
                damageEvent(ctx, d -> x);
            }

            // ---- Efectos sobre entidades ----
            case "potion" -> {
                String effectName = a.require(1, "effect", "type", "potion");
                PotionEffectType type = require(Registries.effect(effectName), "efecto " + effectName);
                long ticks = a.ticks(2, 100, "duration", "time");
                int amp;
                String level = a.get(-1, "level", "nivel");
                if (level != null) amp = (int) Args.number(level) - 1;
                else amp = (int) a.num(3, 0, "amplifier");
                boolean particles = a.bool(4, true, "particles");
                int duration = ticks < 0 ? PotionEffect.INFINITE_DURATION : (int) ticks;
                for (LivingEntity e : living(a.target(0), ctx)) {
                    e.addPotionEffect(new PotionEffect(type, duration, Math.max(0, amp), false, particles));
                }
            }
            case "clear_potion" -> {
                String effectName = a.get(1, "effect", "type", "potion");
                PotionEffectType type = effectName != null ? require(Registries.effect(effectName), "efecto " + effectName) : null;
                for (LivingEntity e : living(a.target(0), ctx)) {
                    if (type != null) e.removePotionEffect(type);
                    else for (PotionEffect pe : e.getActivePotionEffects()) e.removePotionEffect(pe.getType());
                }
            }
            case "heal" -> {
                double amount = a.num(1, "amount", "value");
                for (LivingEntity e : living(a.target(0), ctx)) {
                    AttributeInstance max = e.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    double limit = max != null ? max.getValue() : e.getHealth() + amount;
                    e.setHealth(Math.max(0, Math.min(limit, e.getHealth() + amount)));
                }
            }
            case "damage" -> {
                double amount = a.num(1, "amount", "value");
                Entity source = ctx.origin();
                for (Entity e : select(a.target(0), ctx)) {
                    if (!(e instanceof Damageable d)) continue;
                    if (source != null && source != e) d.damage(amount, source);
                    else d.damage(amount);
                }
            }
            case "ignite" -> {
                int ticks = (int) a.ticks(1, 100, "duration", "time");
                for (Entity e : select(a.target(0), ctx)) e.setFireTicks(ticks);
            }
            case "velocity" -> {
                Vector v = new Vector(a.num(1, 0, "x"), a.num(2, 0, "y"), a.num(3, 0, "z"));
                for (Entity e : select(a.target(0), ctx)) e.setVelocity(v);
            }
            case "knockback" -> {
                Location from = ctx.originLocation();
                double strength = a.num(1, "strength", "value");
                double y = a.num(2, 0.4, "y");
                if (from == null) return;
                for (Entity e : select(a.target(0), ctx)) push(e, from, strength, y);
            }
            case "leap" -> {
                Entity towards = role(a.require(1, "towards"), ctx);
                double strength = a.num(2, 1, "strength");
                double y = a.num(3, 0.5, "y");
                for (Entity e : select(a.target(0), ctx)) push(e, towards.getLocation(), -strength, y);
            }
            case "teleport" -> {
                Entity to = role(a.require(1, "towards"), ctx);
                for (Entity e : select(a.target(0), ctx)) e.teleport(to.getLocation());
            }
            case "target" -> {
                Entity to = ctx.role(a.require(1, "towards").toLowerCase(Locale.ROOT));
                for (Entity e : select(a.target(0), ctx)) {
                    if (e instanceof Mob m) m.setTarget(to instanceof LivingEntity l ? l : null);
                }
            }
            case "lightning" -> {
                boolean fake = a.bool(1, false, "fake");
                for (Entity e : select(a.target(0), ctx)) {
                    if (fake) e.getWorld().strikeLightningEffect(e.getLocation());
                    else e.getWorld().strikeLightning(e.getLocation());
                }
            }
            case "explosion" -> {
                float power = (float) a.num(1, "power");
                boolean fire = a.bool(2, false, "fire");
                boolean breakBlocks = a.bool(3, false, "break");
                for (Entity e : select(a.target(0), ctx)) {
                    e.getWorld().createExplosion(e.getLocation(), power, fire, breakBlocks, ctx.origin());
                }
            }

            // ---- Mensajes, sonidos y partículas ----
            case "message" -> {
                String msg = Text.color(a.text(1, "text"));
                for (Entity e : select(a.target(0), ctx)) e.sendMessage(msg);
            }
            case "actionbar" -> {
                String msg = a.text(1, "text");
                for (Entity e : select(a.target(0), ctx)) {
                    if (e instanceof Player p) Text.actionbar(p, msg);
                }
            }
            case "title" -> {
                String title = Text.color(a.require(1, "title"));
                String subText = a.get(2, "subtitle");
                String sub = subText != null ? Text.color(subText) : "";
                int stay = (int) a.ticks(3, 50, "duration");
                for (Entity e : select(a.target(0), ctx)) {
                    if (e instanceof Player p) p.sendTitle(title, sub, 5, stay, 10);
                }
            }
            case "broadcast" -> Bukkit.broadcastMessage(Text.color(a.text(0, "text")));
            case "sound" -> {
                String soundName = a.require(1, "sound").toLowerCase(Locale.ROOT);
                float volume = (float) a.num(2, 1, "volume");
                float pitch = (float) a.num(3, 1, "pitch");
                for (Entity e : select(a.target(0), ctx)) {
                    e.getWorld().playSound(e.getLocation(), soundName, SoundCategory.MASTER, volume, pitch);
                }
            }
            case "particle" -> {
                String particleName = a.require(1, "particle", "type");
                Particle particle = require(Registries.particle(particleName), "partícula " + particleName);
                int count = (int) a.num(2, 10, "count", "amount");
                double spread = a.num(3, 0.5, "spread", "radius");
                Object data = null;
                if (particle.getDataType() == Particle.DustOptions.class) {
                    String color = a.get(4, "color");
                    String hex = color != null ? color.replace("#", "") : "ffffff";
                    data = new Particle.DustOptions(Color.fromRGB(Integer.parseInt(hex, 16)), 1.5f);
                } else if (particle.getDataType() != Void.class) {
                    throw new IllegalArgumentException("la partícula " + particleName + " necesita datos extra");
                }
                for (Entity e : select(a.target(0), ctx)) {
                    Location l = e.getLocation().add(0, e.getHeight() / 2, 0);
                    e.getWorld().spawnParticle(particle, l, count, spread, spread, spread, 0.02, data);
                }
            }

            // ---- Comandos ----
            case "command" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripSlash(a.text(0, "command")));
            case "player_command" -> {
                String cmd = stripSlash(a.text(1, "command"));
                for (Entity e : select(a.target(0), ctx)) {
                    if (e instanceof Player p) p.performCommand(cmd);
                }
            }

            // ---- Mobs, modelos y scripts ----
            case "spawn" -> {
                String entityName = a.require(0, "entity", "type");
                EntityType type = require(Registries.entity(entityName), "entidad " + entityName);
                for (Location l : locations(a.get(1, "at"), ctx)) l.getWorld().spawnEntity(l, type);
            }
            case "spawn_mob" -> {
                String id = a.require(0, "mob", "id");
                for (Location l : locations(a.get(1, "at"), ctx)) {
                    if (plugin.mobs().spawn(id, l) == null) {
                        throw new IllegalArgumentException("no existe el mob " + id);
                    }
                }
            }
            case "animate" -> {
                String animation = a.require(1, "animation");
                for (Entity e : select(a.target(0), ctx)) plugin.models().animate(e, animation);
            }
            case "toggle_script" -> plugin.scripts().setEnabled(a.require(0, "script"), a.bool(1, true, "enabled", "value"));
            default -> throw new IllegalArgumentException("acción desconocida");
        }
    }

    // ------------------------------------------------------------------ selección de objetivos

    /**
     * Objetivos: un rol (player, victim, attacker, entity, target, killer),
     * all (todos los jugadores), world_players, nearby:radio, nearby_players:radio.
     */
    private List<Entity> select(String selector, Context ctx) {
        String sel = selector.toLowerCase(Locale.ROOT);
        List<Entity> out = new ArrayList<>();
        if (sel.equals("all")) {
            out.addAll(Bukkit.getOnlinePlayers());
            return out;
        }
        Entity origin = ctx.origin();
        if (sel.equals("world_players")) {
            if (origin != null) out.addAll(origin.getWorld().getPlayers());
            return out;
        }
        if (sel.startsWith("nearby:") || sel.startsWith("nearby_players:")) {
            if (origin == null) return out;
            boolean onlyPlayers = sel.startsWith("nearby_players:");
            double r = Args.number(sel.substring(sel.indexOf(':') + 1));
            for (Entity e : origin.getNearbyEntities(r, r, r)) {
                if (!(e instanceof LivingEntity)) continue;
                if (onlyPlayers && !(e instanceof Player)) continue;
                if (e.getLocation().distanceSquared(origin.getLocation()) <= r * r) out.add(e);
            }
            return out;
        }
        Entity e = ctx.role(sel);
        if (e != null && e.isValid()) out.add(e);
        return out;
    }

    private List<LivingEntity> living(String selector, Context ctx) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : select(selector, ctx)) {
            if (e instanceof LivingEntity l) out.add(l);
        }
        return out;
    }

    private List<Location> locations(String selector, Context ctx) {
        List<Location> out = new ArrayList<>();
        if (selector == null) {
            Location l = ctx.originLocation();
            if (l != null) out.add(l);
        } else {
            for (Entity e : select(selector, ctx)) out.add(e.getLocation());
        }
        return out;
    }

    private static Entity role(String name, Context ctx) {
        return require(ctx.role(name.toLowerCase(Locale.ROOT)), "rol " + name);
    }

    // ------------------------------------------------------------------ utilidades

    private void damageEvent(Context ctx, java.util.function.DoubleUnaryOperator op) {
        if (ctx.event() instanceof EntityDamageEvent e) {
            e.setDamage(Math.max(0, op.applyAsDouble(e.getDamage())));
        }
    }

    /** Empuja alejándose de "from" (fuerza negativa = hacia "from"). */
    private static void push(Entity e, Location from, double strength, double y) {
        if (from.getWorld() != e.getWorld()) return;
        Vector dir = e.getLocation().toVector().subtract(from.toVector()).setY(0);
        if (dir.lengthSquared() < 1.0E-4) return;
        dir.normalize().multiply(strength).setY(y);
        e.setVelocity(dir);
    }

    private static String stripSlash(String cmd) {
        return cmd.startsWith("/") ? cmd.substring(1) : cmd;
    }

    private static <T> T require(T value, String what) {
        if (value == null) throw new IllegalArgumentException("no existe " + what);
        return value;
    }
}
