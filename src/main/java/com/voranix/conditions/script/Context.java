package com.voranix.conditions.script;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Datos de un evento: entidades con nombre (roles) y variables sueltas.
 */
public final class Context {

    private static final String[] ORIGIN_ORDER = {"entity", "attacker", "player", "victim", "killer", "target"};

    private final Map<String, Entity> roles = new LinkedHashMap<>();
    private final Map<String, String> vars = new HashMap<>();
    private Event event;

    public Context role(String name, Entity entity) {
        if (entity != null) roles.put(name, entity);
        return this;
    }

    public Entity role(String name) {
        return roles.get(name);
    }

    public Context var(String key, Object value) {
        vars.put(key, Expression.format(value));
        return this;
    }

    public String var(String key) {
        return vars.get(key);
    }

    public Context event(Event event) {
        this.event = event;
        return this;
    }

    public Event event() {
        return event;
    }

    /** Entidad "principal" del evento, usada para nearby:, knockback, sonidos globales, etc. */
    public Entity origin() {
        for (String key : ORIGIN_ORDER) {
            Entity e = roles.get(key);
            if (e != null) return e;
        }
        return roles.isEmpty() ? null : roles.values().iterator().next();
    }

    public Location originLocation() {
        Entity o = origin();
        return o == null ? null : o.getLocation();
    }

    /** Copia sin el evento: para acciones que corren después de un delay, cuando el evento ya terminó. */
    public Context detached() {
        Context c = new Context();
        c.roles.putAll(roles);
        c.vars.putAll(vars);
        return c;
    }
}
