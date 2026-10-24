package com.voranix.conditions.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Una acción ya leída del YAML. Se puede escribir de tres formas:
 * <pre>
 *   - damage_multiply: 1.5              # valor principal
 *   - potion:                           # parámetros con nombre
 *       target: victim
 *       effect: poison
 *       level: 2
 *       duration: 5s
 *   - "potion victim poison 100 1"      # formato antiguo en una línea
 * </pre>
 */
public record Action(String name, List<String> positional, Map<String, String> named, String raw) {

    /** Parámetro que recibe el valor de "- accion: valor". */
    static final Map<String, String> MAIN = new HashMap<>();

    static {
        for (String n : List.of("damage_multiply", "damage_add", "damage_set")) MAIN.put(n, "value");
        MAIN.put("potion", "effect");
        MAIN.put("clear_potion", "effect");
        MAIN.put("heal", "amount");
        MAIN.put("damage", "amount");
        MAIN.put("ignite", "duration");
        MAIN.put("knockback", "strength");
        MAIN.put("leap", "towards");
        MAIN.put("teleport", "towards");
        MAIN.put("target", "towards");
        MAIN.put("explosion", "power");
        MAIN.put("message", "text");
        MAIN.put("actionbar", "text");
        MAIN.put("title", "title");
        MAIN.put("broadcast", "text");
        MAIN.put("sound", "sound");
        MAIN.put("particle", "particle");
        MAIN.put("command", "command");
        MAIN.put("player_command", "command");
        MAIN.put("spawn", "entity");
        MAIN.put("spawn_mob", "mob");
        MAIN.put("animate", "animation");
        MAIN.put("toggle_script", "script");
        MAIN.put("delay", "duration");
        MAIN.put("velocity", "target");
        MAIN.put("lightning", "target");
        MAIN.put("cancel", "value");
    }

    public static boolean known(String name) {
        return MAIN.containsKey(name);
    }

    public static Action parse(Object o) {
        if (o instanceof Map<?, ?> map && !map.isEmpty()) {
            var entries = new ArrayList<>(map.entrySet());
            String name = entries.getFirst().getKey().toString().toLowerCase(Locale.ROOT);
            Object value = entries.getFirst().getValue();
            Map<String, String> named = new LinkedHashMap<>();
            if (value instanceof Map<?, ?> params) {
                putAll(named, params);
            } else if (value != null) {
                named.put(MAIN.getOrDefault(name, "value"), value.toString());
            }
            // Forma "- potion: poison" con más claves al mismo nivel (target:, level:, ...)
            for (int i = 1; i < entries.size(); i++) {
                named.put(entries.get(i).getKey().toString().toLowerCase(Locale.ROOT), str(entries.get(i).getValue()));
            }
            return new Action(name, List.of(), named, map.toString());
        }
        String line = String.valueOf(o);
        List<String> tokens = tokenize(line);
        if (tokens.isEmpty()) throw new IllegalArgumentException("acción vacía");
        return new Action(tokens.getFirst().toLowerCase(Locale.ROOT), tokens.subList(1, tokens.size()), Map.of(), line);
    }

    private static void putAll(Map<String, String> out, Map<?, ?> params) {
        for (Map.Entry<?, ?> e : params.entrySet()) {
            out.put(e.getKey().toString().toLowerCase(Locale.ROOT), str(e.getValue()));
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    /** Divide por espacios respetando "texto entre comillas". */
    static List<String> tokenize(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        char quote = 0;
        boolean has = false;
        for (char c : line.toCharArray()) {
            if (quote != 0) {
                if (c == quote) quote = 0;
                else cur.append(c);
            } else if (c == '"' || c == '\'') {
                quote = c;
                has = true;
            } else if (Character.isWhitespace(c)) {
                if (has || !cur.isEmpty()) out.add(cur.toString());
                cur.setLength(0);
                has = false;
            } else {
                cur.append(c);
                has = true;
            }
        }
        if (has || !cur.isEmpty()) out.add(cur.toString());
        return out;
    }
}
