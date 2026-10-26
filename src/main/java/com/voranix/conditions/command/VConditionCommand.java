package com.voranix.conditions.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.voranix.conditions.VoranixConditions;
import com.voranix.conditions.mob.CustomMob;
import com.voranix.conditions.mob.MobManager;
import com.voranix.conditions.script.Script;
import com.voranix.conditions.script.Text;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /vcondition script <nombre> [true|false]
 * /vcondition list | reload | debug
 * /vcondition mob spawn <id> [jugador] | mob list | mob kill [id]
 */
@CommandAlias("vcondition|vc")
@CommandPermission("voranixconditions.admin")
@Description("Administra scripts y mobs de VoranixConditions")
public final class VConditionCommand extends BaseCommand {

    private static final String PREFIX = "<dark_gray>[<gradient:#a855f7:#6366f1>VConditions</gradient>]</dark_gray> ";

    private final VoranixConditions plugin;

    public VConditionCommand(VoranixConditions plugin) {
        this.plugin = plugin;
    }

    @Default
    @Subcommand("help")
    public void help(CommandSender sender) {
        send(sender, "<gray>Comandos:");
        for (String line : List.of(
                "/vcondition script <nombre> <true|false>",
                "/vcondition list",
                "/vcondition reload",
                "/vcondition mob spawn <id> [jugador]",
                "/vcondition mob list",
                "/vcondition mob kill [id]",
                "/vcondition debug")) {
            Text.send(sender, " <white>" + line);
        }
    }

    @Subcommand("script")
    @Syntax("<nombre> [true|false]")
    @CommandCompletion("@scripts true|false")
    public void script(CommandSender sender, String name, @Optional String value) {
        Script script = plugin.scripts().get(name);
        if (script == null) {
            send(sender, "<red>No existe el script <white>" + name);
            return;
        }
        if (value == null) {
            send(sender, "<gray>" + script.name() + ": " + state(plugin.scripts().isEnabled(script.name())));
            return;
        }
        String v = value.toLowerCase(Locale.ROOT);
        if (!v.equals("true") && !v.equals("false")) {
            send(sender, "<red>El valor debe ser <white>true</white> o <white>false");
            return;
        }
        boolean enabled = v.equals("true");
        plugin.scripts().setEnabled(script.name(), enabled);
        send(sender, "<gray>Script <white>" + script.name() + "</white> " + state(enabled));
    }

    @Subcommand("list")
    public void list(CommandSender sender) {
        send(sender, "<gray>Scripts:");
        for (Script s : plugin.scripts().all()) {
            Text.send(sender, " " + state(plugin.scripts().isEnabled(s.name())) + " <white>" + s.name()
                    + (s.description().isEmpty() ? "" : " <dark_gray>- <gray>" + s.description()));
        }
        if (plugin.scripts().all().isEmpty()) Text.send(sender, " <dark_gray>(ninguno)");
    }

    @Subcommand("reload")
    public void reload(CommandSender sender) {
        plugin.reload();
        send(sender, "<green>Scripts y mobs recargados.");
    }

    @Subcommand("debug")
    public void debug(CommandSender sender) {
        boolean on = !plugin.actions().debug();
        plugin.actions().debug(on);
        send(sender, "<gray>Debug " + (on ? "<green>activado<gray>: mira la consola al probar" : "<red>desactivado"));
    }

    @Subcommand("mob spawn")
    @Syntax("<id> [jugador]")
    @CommandCompletion("@mobs @players")
    public void mobSpawn(CommandSender sender, String id, @Optional OnlinePlayer target) {
        Location at;
        if (target != null) at = target.getPlayer().getLocation();
        else if (sender instanceof Player p) at = p.getLocation();
        else {
            send(sender, "<red>Desde la consola indica un jugador: /vcondition mob spawn <id> <jugador>");
            return;
        }
        LivingEntity e = plugin.mobs().spawn(id, at);
        send(sender, e != null ? "<green>Spawneado <white>" + id : "<red>No existe el mob <white>" + id);
    }

    @Subcommand("mob list")
    public void mobList(CommandSender sender) {
        send(sender, "<gray>Mobs personalizados:");
        for (CustomMob m : plugin.mobs().all()) {
            long alive = plugin.mobs().live().stream().filter(e -> m.id().equalsIgnoreCase(MobManager.idOf(e))).count();
            Text.send(sender, " <white>" + m.id() + " <dark_gray>(" + m.base().getKey().getKey()
                    + (m.model() != null ? ", modelo " + m.model() : "") + ") <gray>vivos: " + alive);
        }
    }

    @Subcommand("mob kill")
    @Syntax("[id]")
    @CommandCompletion("@mobs")
    public void mobKill(CommandSender sender, @Optional String id) {
        int count = 0;
        for (LivingEntity e : new ArrayList<>(plugin.mobs().live())) {
            if (id == null || id.equalsIgnoreCase(MobManager.idOf(e))) {
                e.remove();
                plugin.mobs().untrack(e);
                count++;
            }
        }
        send(sender, "<gray>Eliminados <white>" + count + "</white> mobs.");
    }

    private static String state(boolean enabled) {
        return enabled ? "<green>● activo" : "<red>○ inactivo";
    }

    private static void send(CommandSender sender, String msg) {
        Text.send(sender, PREFIX + msg);
    }
}
