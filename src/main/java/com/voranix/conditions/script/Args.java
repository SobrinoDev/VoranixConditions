package com.voranix.conditions.script;

import java.util.List;
import java.util.Locale;

/**
 * Lee los parámetros de una acción: primero por nombre (formato con ":"),
 * si no, por posición (formato antiguo en una línea). Los placeholders se resuelven al leer.
 */
final class Args {

    static final String[] TARGET = {"target", "trigger", "who"};

    private final Action action;
    private final Context ctx;

    Args(Action action, Context ctx) {
        this.action = action;
        this.ctx = ctx;
    }

    /** Valor del parámetro o null. pos = índice en el formato antiguo (-1 si no aplica). */
    String get(int pos, String... names) {
        for (String n : names) {
            String v = action.named().get(n);
            if (v != null) return Expression.resolve(v, ctx);
        }
        List<String> p = action.positional();
        if (pos >= 0 && pos < p.size()) return Expression.resolve(p.get(pos), ctx);
        return null;
    }

    String require(int pos, String... names) {
        String v = get(pos, names);
        if (v == null || v.isEmpty()) throw new IllegalArgumentException("falta '" + names[0] + "'");
        return v;
    }

    /** Para textos: el parámetro con nombre o todas las palabras desde la posición pos. */
    String text(int pos, String... names) {
        for (String n : names) {
            String v = action.named().get(n);
            if (v != null) return Expression.resolve(v, ctx);
        }
        List<String> p = action.positional();
        if (pos >= p.size()) throw new IllegalArgumentException("falta '" + names[0] + "'");
        StringBuilder sb = new StringBuilder();
        for (int i = pos; i < p.size(); i++) {
            if (i > pos) sb.append(' ');
            sb.append(Expression.resolve(p.get(i), ctx));
        }
        return sb.toString();
    }

    String target(int pos) {
        return require(pos, TARGET);
    }

    double num(int pos, String... names) {
        return number(require(pos, names));
    }

    double num(int pos, double def, String... names) {
        String v = get(pos, names);
        return v == null || v.isEmpty() ? def : number(v);
    }

    boolean bool(int pos, boolean def, String... names) {
        String v = get(pos, names);
        return v == null || v.isEmpty() ? def : Boolean.parseBoolean(v);
    }

    /** Duración en ticks: "100" (ticks), "20t", "5s", "2m", "1h" o "infinite". */
    long ticks(int pos, long def, String... names) {
        String v = get(pos, names);
        return v == null || v.isEmpty() ? def : ticks(v);
    }

    static long ticks(String raw) {
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if (v.equals("infinite") || v.equals("infinito")) return -1;
        double mult = 1;
        char unit = v.charAt(v.length() - 1);
        if (Character.isLetter(unit)) {
            mult = switch (unit) {
                case 't' -> 1;
                case 's' -> 20;
                case 'm' -> 20 * 60;
                case 'h' -> 20 * 3600;
                default -> throw new IllegalArgumentException("unidad de tiempo '" + unit + "' no válida (usa t, s, m, h)");
            };
            v = v.substring(0, v.length() - 1);
        }
        return Math.round(number(v) * mult);
    }

    static double number(String s) {
        Double d = Expression.number(s.trim());
        if (d == null) throw new IllegalArgumentException("'" + s + "' no es un número");
        return d;
    }
}
