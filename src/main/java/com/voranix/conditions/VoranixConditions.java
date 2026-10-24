package com.voranix.conditions;

import com.voranix.conditions.command.VConditionCommand;
import com.voranix.conditions.mob.MobManager;
import com.voranix.conditions.model.BetterModelBridge;
import com.voranix.conditions.model.ModelBridge;
import com.voranix.conditions.script.ActionRunner;
import com.voranix.conditions.script.ScriptManager;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class VoranixConditions extends JavaPlugin {

    private ScriptManager scripts;
    private MobManager mobs;
    private ActionRunner actions;
    private ModelBridge models = ModelBridge.NONE;
    private long tick;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().isPluginEnabled("BetterModel")) {
            models = new BetterModelBridge();
            getLogger().info("BetterModel detectado: modelos activados.");
        } else {
            getLogger().warning("BetterModel no está instalado: los mobs se verán con su modelo vanilla.");
        }

        actions = new ActionRunner(this);
        scripts = new ScriptManager(this);
        mobs = new MobManager(this);
        reload();

        getServer().getPluginManager().registerEvents(new EventBridge(this), this);
        getServer().getScheduler().runTaskTimer(this, () -> {
            tick++;
            scripts.tick(tick);
            mobs.tick(tick);
        }, 1L, 1L);

        registerCommand("vcondition", "Administra scripts y mobs de VoranixConditions", List.of("vc"),
                new VConditionCommand(this));
    }

    public void reload() {
        scripts.load();
        mobs.load();
        // Mobs que ya estaban cargados (por ejemplo tras /reload)
        for (World world : getServer().getWorlds()) {
            for (LivingEntity e : world.getLivingEntities()) {
                if (MobManager.idOf(e) != null) mobs.track(e);
            }
        }
    }

    public ScriptManager scripts() {
        return scripts;
    }

    public MobManager mobs() {
        return mobs;
    }

    public ActionRunner actions() {
        return actions;
    }

    public ModelBridge models() {
        return models;
    }
}
