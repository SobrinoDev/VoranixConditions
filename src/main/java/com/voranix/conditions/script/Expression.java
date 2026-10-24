package com.voranix.conditions.script;

import com.voranix.conditions.mob.MobManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Placeholders ({rol.propiedad}, {variable}) y evaluación de condiciones.
 */
public final class Expression {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z_]+)(?:\\.([^}]+))?}");
    private static final String[] OPERATORS = {" contains ", ">=", "<=", "!=", "==", ">", "<"};

    private Expression() {
    }

    public static String resolve(String text, Context ctx) {
        if (text.indexOf('{') < 0) return text;
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String value = value(m.group(1).toLowerCase(Locale.ROOT), m.group(2), ctx);
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Evalúa "a == b && c > d || e". Los || separan alternativas y los && se exigen todos. */
    public static boolean test(String condition, Context ctx) {
        String resolved = resolve(condition, ctx);
        for (String alt : resolved.split("\\|\\|")) {
            boolean all = true;
            for (String part : alt.split("&&")) {
                if (!compare(part.trim())) {
                    all = false;
                    break;
                }
            }
            if (all) return true;
        }
        return false;
    }

    private static boolean compare(String s) {
        boolean negate = false;
        if (s.startsWith("!") && !s.startsWith("!=")) {
            negate = true;
            s = s.substring(1).trim();
        }
        for (String op : OPERATORS) {
            int i = s.indexOf(op);
            if (i <= 0) continue;
            String left = unquote(s.substring(0, i).trim());
            String right = unquote(s.substring(i + op.length()).trim());
            return negate ^ compare(left, op.trim(), right);
        }
        return negate ^ unquote(s).equalsIgnoreCase("true");
    }

    private static boolean compare(String left, String op, String right) {
        Double a = number(left);
        Double b = number(right);
        if (a != null && b != null) {
            return switch (op) {
                case ">=" -> a >= b;
                case "<=" -> a <= b;
                case ">" -> a > b;
                case "<" -> a < b;
                case "!=" -> !a.equals(b);
                case "contains" -> left.contains(right);
                default -> a.equals(b);
            };
        }
        return switch (op) {
            case "!=" -> !left.equalsIgnoreCase(right);
            case "==" -> left.equalsIgnoreCase(right);
            case "contains" -> left.toLowerCase(Locale.ROOT).contains(right.toLowerCase(Locale.ROOT));
            default -> false;
        };
    }

    private static String value(String head, String prop, Context ctx) {
        if (prop == null) {
            return switch (head) {
                case "random" -> format(ThreadLocalRandom.current().nextDouble());
                case "online" -> String.valueOf(Bukkit.getOnlinePlayers().size());
                default -> {
                    String v = ctx.var(head);
                    if (v != null) yield v;
                    Entity e = ctx.role(head);
                    yield e != null ? entityName(e) : "none";
                }
            };
        }
        if (head.equals("world")) {
            Entity o = ctx.origin();
            World w = o != null ? o.getWorld() : Bukkit.getWorlds().getFirst();
            return worldProp(w, prop.toLowerCase(Locale.ROOT));
        }
        return entityProp(ctx.role(head), prop, ctx);
    }

    private static String worldProp(World w, String prop) {
        return switch (prop) {
            case "name" -> w.getName();
            case "time" -> String.valueOf(w.getTime());
            case "day" -> String.valueOf(w.getFullTime() / 24000);
            case "storm" -> String.valueOf(w.hasStorm());
            case "thunder" -> String.valueOf(w.isThundering());
            case "players" -> String.valueOf(w.getPlayers().size());
            case "difficulty" -> w.getDifficulty().name();
            default -> "none";
        };
    }

    private static String entityProp(Entity e, String rawProp, Context ctx) {
        String prop = rawProp;
        String arg = null;
        int colon = rawProp.indexOf(':');
        if (colon > 0) {
            prop = rawProp.substring(0, colon);
            arg = rawProp.substring(colon + 1);
        }
        prop = prop.toLowerCase(Locale.ROOT);
        if (e == null) return prop.equals("exists") ? "false" : "none";

        LivingEntity living = e instanceof LivingEntity l ? l : null;
        Player player = e instanceof Player p ? p : null;
        return switch (prop) {
            case "exists" -> "true";
            case "name" -> entityName(e);
            case "uuid" -> e.getUniqueId().toString();
            case "type" -> e.getType().getKey().getKey().toUpperCase(Locale.ROOT);
            case "custom_mob" -> {
                String id = MobManager.idOf(e);
                yield id != null ? id : "none";
            }
            case "is_player" -> String.valueOf(player != null);
            case "world" -> e.getWorld().getName();
            case "x" -> format(e.getLocation().getX());
            case "y" -> format(e.getLocation().getY());
            case "z" -> format(e.getLocation().getZ());
            case "biome" -> e.getLocation().getBlock().getBiome().getKey().getKey().toUpperCase(Locale.ROOT);
            case "light" -> String.valueOf(e.getLocation().getBlock().getLightLevel());
            case "health" -> living != null ? format(living.getHealth()) : "0";
            case "max_health" -> {
                AttributeInstance a = living != null ? living.getAttribute(Attribute.MAX_HEALTH) : null;
                yield a != null ? format(a.getValue()) : "0";
            }
            case "on_fire" -> String.valueOf(e.getFireTicks() > 0);
            case "in_water" -> String.valueOf(e.isInWater());
            case "on_ground" -> String.valueOf(e.isOnGround());
            case "tag" -> String.valueOf(arg != null && e.getScoreboardTags().contains(arg));
            case "has_effect" -> {
                PotionEffectType t = arg != null ? Registries.effect(arg) : null;
                yield String.valueOf(living != null && t != null && living.hasPotionEffect(t));
            }
            case "effect_level" -> {
                PotionEffectType t = arg != null ? Registries.effect(arg) : null;
                PotionEffect pe = living != null && t != null ? living.getPotionEffect(t) : null;
                yield pe != null ? String.valueOf(pe.getAmplifier() + 1) : "0";
            }
            case "distance" -> {
                Entity other = arg != null ? ctx.role(arg.toLowerCase(Locale.ROOT)) : null;
                if (other == null || other.getWorld() != e.getWorld()) yield "999999";
                yield format(other.getLocation().distance(e.getLocation()));
            }
            case "has_target" -> String.valueOf(e instanceof Mob m && m.getTarget() != null);
            case "holding" -> living != null && living.getEquipment() != null
                    ? living.getEquipment().getItemInMainHand().getType().getKey().getKey().toUpperCase(Locale.ROOT)
                    : "AIR";
            case "permission" -> String.valueOf(player != null && arg != null && player.hasPermission(arg));
            case "gamemode" -> player != null ? player.getGameMode().name() : "none";
            case "sneaking" -> String.valueOf(player != null && player.isSneaking());
            case "sprinting" -> String.valueOf(player != null && player.isSprinting());
            case "level" -> player != null ? String.valueOf(player.getLevel()) : "0";
            case "food" -> player != null ? String.valueOf(player.getFoodLevel()) : "0";
            default -> "none";
        };
    }

    private static String entityName(Entity e) {
        if (e instanceof Player p) return p.getName();
        return PlainTextComponentSerializer.plainText().serialize(e.name());
    }

    /** Números enteros sin ".0" y decimales con máximo 2 cifras. */
    public static String format(Object value) {
        if (value instanceof Number n) {
            double d = n.doubleValue();
            if (d == Math.rint(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
            return String.valueOf(Math.round(d * 100.0) / 100.0);
        }
        return String.valueOf(value);
    }

    public static Double number(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && (s.startsWith("\"") && s.endsWith("\"") || s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
