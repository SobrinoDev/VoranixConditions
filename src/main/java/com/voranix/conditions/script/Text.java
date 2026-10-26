package com.voranix.conditions.script;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Spigot no trae Adventure: los textos se escriben en MiniMessage ("<red>", "<gradient:...>")
 * y se convierten a colores clásicos (§). También se aceptan códigos "&c".
 */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private Text() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', LEGACY.serialize(MM.deserialize(text)));
    }

    public static void send(CommandSender sender, String text) {
        sender.sendMessage(color(text));
    }

    public static void actionbar(Player player, String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(color(text)));
    }
}
