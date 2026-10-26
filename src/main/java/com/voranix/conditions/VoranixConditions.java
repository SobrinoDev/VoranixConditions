package com.voranix.conditions;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.Locales;
import com.voranix.conditions.command.VConditionCommand;
import com.voranix.conditions.mob.CustomMob;
import com.voranix.conditions.mob.MobManager;
import com.voranix.conditions.model.BetterModelBridge;
import com.voranix.conditions.model.ModelBridge;
import com.voranix.conditions.script.ActionRunner;
import com.voranix.conditions.script.Script;
import com.voranix.conditions.script.ScriptManager;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

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

        registerCommands();
    }

    private void registerCommands() {
        BukkitCommandManager manager = new BukkitCommandManager(this);
        manager.getLocales().setDefaultLocale(Locales.SPANISH);
        manager.getCommandCompletions().registerCompletion("scripts",
                c -> scripts.all().stream().map(Script::name).toList());
        manager.getCommandCompletions().registerCompletion("mobs",
                c -> mobs.all().stream().map(CustomMob::id).toList());
        manager.registerCommand(new VConditionCommand(this));
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
