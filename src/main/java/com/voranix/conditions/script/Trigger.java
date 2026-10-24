package com.voranix.conditions.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Un bloque "on: evento" con sus condiciones, efectos y acciones.
 * Se usa tanto en los scripts como en los comportamientos de los mobs.
 */
public record Trigger(String event, int interval, String per, List<String> conditions, List<Action> actions) {

    public static Trigger fromMap(Map<?, ?> map, String source, Logger log) {
        // YAML 1.1 lee la clave "on" como el booleano true, así que se busca de las dos formas
        Object on = map.containsKey("on") ? map.get("on") : map.get(Boolean.TRUE);
        if (on == null) on = map.get("event");
        if (on == null) {
            throw new IllegalArgumentException("falta 'on:'");
        }
        String event = on.toString().toLowerCase(Locale.ROOT);
        int interval = map.get("interval") instanceof Number n ? Math.max(1, n.intValue()) : 20;
        if (map.get("interval") instanceof String s) interval = (int) Math.max(1, Args.ticks(s));
        String per = map.get("per") != null ? map.get("per").toString().toLowerCase(Locale.ROOT) : "player";

        List<Action> actions = new ArrayList<>();
        Object effects = map.containsKey("effects") ? map.get("effects") : map.get("effect");
        if (effects != null) actions.addAll(effects(effects, source + " (" + event + ")", log));
        if (map.get("actions") instanceof List<?> list) {
            for (Object o : list) {
                Action action = parseAction(o, source + " (" + event + ")", log);
                if (action != null) actions.add(action);
            }
        }
        return new Trigger(event, interval, per, stringList(map.get("conditions")), actions);
    }

    public static List<Trigger> fromList(List<Map<?, ?>> list, String source, Logger log) {
        List<Trigger> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            try {
                out.add(fromMap(list.get(i), source, log));
            } catch (Exception e) {
                log.warning(source + ": trigger #" + (i + 1) + " inválido (" + e.getMessage() + ")");
            }
        }
        return out;
    }

    private static Action parseAction(Object o, String where, Logger log) {
        try {
            Action action = Action.parse(o);
            if (!Action.known(action.name())) {
                log.warning(where + ": acción desconocida '" + action.name() + "'");
                return null;
            }
            return action;
        } catch (Exception e) {
            log.warning(where + ": acción inválida " + o + " (" + e.getMessage() + ")");
            return null;
        }
    }

    /**
     * Sección effects:
     * <pre>
     * effects:
     *   potion:            # un efecto (mapa) o varios (lista)
     *     type: poison
     *     target: victim
     *     level: 2
     *     duration: 5s
     * </pre>
     */
    private static List<Action> effects(Object section, String where, Logger log) {
        List<Action> out = new ArrayList<>();
        if (!(section instanceof Map<?, ?> map)) {
            log.warning(where + ": 'effects' debe tener subcategorías como potion:");
            return out;
        }
        for (Map.Entry<?, ?> e : map.entrySet()) {
            String kind = e.getKey().toString().toLowerCase(Locale.ROOT);
            List<?> entries = e.getValue() instanceof List<?> l ? l : List.of(e.getValue());
            for (Object entry : entries) {
                if (!(entry instanceof Map<?, ?> params)) {
                    log.warning(where + ": effects." + kind + " necesita parámetros (type, target, level, duration...)");
                    continue;
                }
                Action action = parseAction(Map.of(kind, params), where, log);
                if (action != null) out.add(action);
            }
        }
        return out;
    }

    private static List<String> stringList(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> l) {
            for (Object x : l) {
                if (x != null) out.add(x.toString());
            }
        } else if (o != null) {
            out.add(o.toString());
        }
        return out;
    }
}
