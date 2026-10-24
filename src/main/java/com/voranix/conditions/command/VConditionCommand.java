package com.voranix.conditions.command;

import com.voranix.conditions.VoranixConditions;
import com.voranix.conditions.mob.CustomMob;
import com.voranix.conditions.mob.MobManager;
import com.voranix.conditions.script.Script;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * /vcondition script <nombre> [true|false]
 * /vcondition list | reload | debug
 * /vcondition mob spawn <id> [jugador] | mob list | mob kill [id]
 */
public final class VConditionCommand implements BasicCommand {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String PREFIX = "<dark_gray>[<gradient:#a855f7:#6366f1>VConditions</gradient>]</dark_gray> ";

    private final VoranixConditions plugin;

    public VConditionCommand(VoranixConditions plugin) {
        this.plugin = plugin;
    }

    @Override
    public String permission() {
        return "voranixconditions.admin";
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            help(sender);
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "script" -> script(sender, args);
            case "list" -> list(sender);
            case "reload" -> {
                plugin.reload();
                send(sender, "<green>Scripts y mobs recargados.");
            }
            case "mob" -> mob(source, args);
            case "debug" -> {
                boolean on = !plugin.actions().debug();
                plugin.actions().debug(on);
                send(sender, "<gray>Debug " + (on ? "<green>activado<gray>: mira la consola al probar" : "<red>desactivado"));
            }
            default -> help(sender);
        }
    }

    private void script(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "<red>Uso: /vcondition script <nombre> <true|false>");
            return;
        }
        Script script = plugin.scripts().get(args[1]);
        if (script == null) {
            send(sender, "<red>No existe el script <white>" + args[1]);
            return;
        }
        if (args.length < 3) {
            send(sender, "<gray>" + script.name() + ": " + state(plugin.scripts().isEnabled(script.name())));
            return;
        }
        String value = args[2].toLowerCase(Locale.ROOT);
        if (!value.equals("true") && !value.equals("false")) {
            send(sender, "<red>El valor debe ser <white>true</white> o <white>false");
            return;
        }
        boolean enabled = value.equals("true");
        plugin.scripts().setEnabled(script.name(), enabled);
        send(sender, "<gray>Script <white>" + script.name() + "</white> " + state(enabled));
    }

    private void list(CommandSender sender) {
        send(sender, "<gray>Scripts:");
        for (Script s : plugin.scripts().all()) {
            sender.sendMessage(MM.deserialize(" " + state(plugin.scripts().isEnabled(s.name())) + " <white>" + s.name()
                    + (s.description().isEmpty() ? "" : " <dark_gray>- <gray>" + s.description())));
        }
        if (plugin.scripts().all().isEmpty()) sender.sendMessage(MM.deserialize(" <dark_gray>(ninguno)"));
    }

    private void mob(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        switch (sub) {
            case "spawn" -> {
                if (args.length < 3) {
                    send(sender, "<red>Uso: /vcondition mob spawn <id> [jugador]");
                    return;
                }
                Location at;
                if (args.length > 3) {
                    Player p = Bukkit.getPlayerExact(args[3]);
                    if (p == null) {
                        send(sender, "<red>Jugador no conectado.");
                        return;
                    }
                    at = p.getLocation();
                } else {
                    at = source.getLocation();
                }
                LivingEntity e = plugin.mobs().spawn(args[2], at);
                send(sender, e != null ? "<green>Spawneado <white>" + args[2] : "<red>No existe el mob <white>" + args[2]);
            }
            case "kill" -> {
                String id = args.length > 2 ? args[2] : null;
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
            default -> {
                send(sender, "<gray>Mobs personalizados:");
                for (CustomMob m : plugin.mobs().all()) {
                    long alive = plugin.mobs().live().stream().filter(e -> m.id().equalsIgnoreCase(MobManager.idOf(e))).count();
                    sender.sendMessage(MM.deserialize(" <white>" + m.id() + " <dark_gray>(" + m.base().getKey().getKey()
                            + (m.model() != null ? ", modelo " + m.model() : "") + ") <gray>vivos: " + alive));
                }
            }
        }
    }

    private void help(CommandSender sender) {
        send(sender, "<gray>Comandos:");
        for (String line : List.of(
                "/vcondition script <nombre> <true|false>",
                "/vcondition list",
                "/vcondition reload",
                "/vcondition mob spawn <id> [jugador]",
                "/vcondition mob list",
                "/vcondition mob kill [id]",
                "/vcondition debug")) {
            sender.sendMessage(MM.deserialize(" <white>" + line));
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length <= 1) return filter(List.of("script", "list", "reload", "mob", "debug"), args);
        String first = args[0].toLowerCase(Locale.ROOT);
        if (first.equals("script")) {
            if (args.length == 2) return filter(plugin.scripts().all().stream().map(Script::name).toList(), args);
            if (args.length == 3) return filter(List.of("true", "false"), args);
        }
        if (first.equals("mob")) {
            if (args.length == 2) return filter(List.of("spawn", "list", "kill"), args);
            if (args.length == 3 && !args[1].equalsIgnoreCase("list")) {
                return filter(plugin.mobs().all().stream().map(CustomMob::id).toList(), args);
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("spawn")) {
                return filter(Bukkit.getOnlinePlayers().stream().map(Entity::getName).toList(), args);
            }
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String[] args) {
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(last)).toList();
    }

    private static String state(boolean enabled) {
        return enabled ? "<green>● activo" : "<red>○ inactivo";
    }

    private static void send(CommandSender sender, String msg) {
        sender.sendMessage(MM.deserialize(PREFIX + msg));
    }
}
