package com.voranix.conditions.script;

import com.voranix.conditions.VoranixConditions;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Carga los scripts de plugins/VoranixConditions/scripts y guarda cuáles están activos.
 */
public final class ScriptManager {

    private static final String[] DEFAULTS = {"scripts/furia_nocturna.yml", "scripts/evento_velocidad.yml"};

    private final VoranixConditions plugin;
    private final File folder;
    private final File statesFile;
    private final Map<String, Script> scripts = new LinkedHashMap<>();
    private final Map<String, Boolean> states = new HashMap<>();

    public ScriptManager(VoranixConditions plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "scripts");
        this.statesFile = new File(plugin.getDataFolder(), "states.yml");
    }

    public void load() {
        if (!folder.exists()) {
            for (String res : DEFAULTS) plugin.saveResource(res, false);
        }
        scripts.clear();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
                    String fallback = file.getName().replaceFirst("\\.ya?ml$", "");
                    String name = yml.getString("name", fallback);
                    Script script = new Script(name, yml.getString("description", ""), yml.getBoolean("enabled", false),
                            Trigger.fromList(yml.getMapList("triggers"), file.getName(), plugin.getLogger()));
                    scripts.put(name.toLowerCase(Locale.ROOT), script);
                } catch (Exception e) {
                    plugin.getLogger().warning("No se pudo cargar " + file.getName() + ": " + e.getMessage());
                }
            }
        }

        states.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(statesFile);
        for (String key : yml.getKeys(false)) states.put(key.toLowerCase(Locale.ROOT), yml.getBoolean(key));
        plugin.getLogger().info(scripts.size() + " scripts cargados.");
    }

    public Collection<Script> all() {
        return scripts.values();
    }

    public Script get(String name) {
        return name == null ? null : scripts.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean isEnabled(String name) {
        Script s = get(name);
        if (s == null) return false;
        return states.getOrDefault(name.toLowerCase(Locale.ROOT), s.enabledByDefault());
    }

    /** Cambia el estado, lo guarda y dispara los triggers "enable"/"disable". Devuelve false si no existe. */
    public boolean setEnabled(String name, boolean enabled) {
        Script script = get(name);
        if (script == null) return false;
        boolean before = isEnabled(name);
        states.put(name.toLowerCase(Locale.ROOT), enabled);
        save();
        if (before != enabled) {
            String event = enabled ? "enable" : "disable";
            for (Trigger t : script.triggers()) {
                if (t.event().equals(event)) runPer(script, t);
            }
        }
        return true;
    }

    /** Dispara un evento en todos los scripts activos. */
    public void fire(String event, Context ctx) {
        for (Script script : scripts.values()) {
            if (!isEnabled(script.name())) continue;
            for (Trigger t : script.triggers()) {
                if (t.event().equals(event)) plugin.actions().fire(script.name(), t, ctx);
            }
        }
    }

    /** Llamado cada tick: ejecuta los triggers "timer". */
    public void tick(long tick) {
        for (Script script : scripts.values()) {
            if (!isEnabled(script.name())) continue;
            for (Trigger t : script.triggers()) {
                if (t.event().equals("timer") && tick % t.interval() == 0) runPer(script, t);
            }
        }
    }

    private void runPer(Script script, Trigger t) {
        if (t.per().equals("none")) {
            plugin.actions().fire(script.name(), t, new Context());
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.actions().fire(script.name(), t, new Context().role("player", p));
        }
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        states.forEach(yml::set);
        try {
            yml.save(statesFile);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar states.yml: " + e.getMessage());
        }
    }
}
