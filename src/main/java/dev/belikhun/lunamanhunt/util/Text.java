package dev.belikhun.lunamanhunt.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Text helper adapted from BoatRacing (MIT).
 * Source: https://github.com/awakcon1234/BoatRacing/blob/main/src/main/java/dev/belikhun/boatracing/util/Text.java
 */
public final class Text {
    private static final Locale LOCALE = Locale.US;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static String prefixMiniMessage = "<gold>⚔</gold> ";
    private static String prefixLegacy = colorize("&6⚔&r ");

    private Text() {
    }

    public static void setPrefixMiniMessage(String miniMessage) {
        prefixMiniMessage = miniMessage == null ? "" : miniMessage;
    }

    public static void setPrefixLegacy(String legacyAmpersand) {
        prefixLegacy = colorize(legacyAmpersand == null ? "" : legacyAmpersand);
    }

    private static String fmtCoord(double v) {
        double r = Math.rint(v);
        if (Math.abs(v - r) < 1e-9) return String.valueOf((long) r);
        return String.format(LOCALE, "%.1f", v);
    }

    public static String fmtPos(org.bukkit.Location loc) {
        if (loc == null) return "(unset)";
        String w = (loc.getWorld() != null ? loc.getWorld().getName() : "?");
        return w + " (" + fmtCoord(loc.getX()) + ", " + fmtCoord(loc.getY()) + ", " + fmtCoord(loc.getZ()) + ")";
    }

    public static String fmtBlock(org.bukkit.block.Block b) {
        if (b == null) return "(unset)";
        String w = (b.getWorld() != null ? b.getWorld().getName() : "?");
        return w + " (" + b.getX() + ", " + b.getY() + ", " + b.getZ() + ")";
    }

    public static String fmtArea(org.bukkit.util.BoundingBox box) {
        if (box == null) return "[unset]";
        return "[" + fmtCoord(box.getMinX()) + ", " + fmtCoord(box.getMinY()) + ", " + fmtCoord(box.getMinZ())
            + " -> " + fmtCoord(box.getMaxX()) + ", " + fmtCoord(box.getMaxY()) + ", " + fmtCoord(box.getMaxZ())
            + "]";
    }

    public static String fmtArea(String worldName, org.bukkit.util.BoundingBox box) {
        String w = (worldName == null || worldName.isEmpty()) ? "?" : worldName;
        return w + " " + fmtArea(box);
    }

    public static String colorize(String s) {
        if (s == null) return "";
        return s.replace('&', '§');
    }

    public static String prefixMiniMessage() {
        return prefixMiniMessage;
    }

    public static String prefixLegacy() {
        return prefixLegacy;
    }

    public static void msg(CommandSender to, String miniMessage) {
        if (to == null) return;
        String base = miniMessage == null ? "" : miniMessage;
        send(to, mm(prefixMiniMessage + base));
    }

    public static void tell(CommandSender to, String miniMessage) {
        if (to == null) return;
        String base = miniMessage == null ? "" : miniMessage;
        send(to, mm(base));
    }

    public static void msgLegacy(CommandSender to, String legacyAmpersand) {
        if (to == null) return;
        String base = legacyAmpersand == null ? "" : legacyAmpersand;
        to.sendMessage(colorize(prefixLegacy + base));
    }

    public static void tellLegacy(CommandSender to, String legacyAmpersand) {
        if (to == null) return;
        String base = legacyAmpersand == null ? "" : legacyAmpersand;
        to.sendMessage(colorize(base));
    }

    public static void send(CommandSender to, Component component) {
        if (to == null || component == null) return;
        to.sendMessage(component);
    }

    public static Component c(String legacyAmpersand) {
        if (legacyAmpersand == null) return Component.empty();
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacyAmpersand);
    }

    public static Component mm(String miniMessage) {
        if (miniMessage == null) return Component.empty();
        return MINI_MESSAGE.deserialize(miniMessage);
    }

    public static List<Component> lore(List<String> lines) {
        if (lines == null) return java.util.Collections.emptyList();
        return lines.stream()
            .map((String s) -> Text.c("&r" + s).decoration(TextDecoration.ITALIC, false))
            .collect(Collectors.toList());
    }

    public static String plain(Component component) {
        if (component == null) return "";
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static Component title(String text) {
        if (text == null) text = "";
        return c("&r" + text).decoration(TextDecoration.ITALIC, false);
    }

    public static Component item(String text) {
        if (text == null) text = "";
        return c("&r" + text).decoration(TextDecoration.ITALIC, false);
    }

    public static Component cmd(String labelLegacy, String command) {
        if (labelLegacy == null) labelLegacy = "";
        if (command == null) command = "";
        Component base = c(labelLegacy).decoration(TextDecoration.ITALIC, false);
        return base
            .clickEvent(ClickEvent.suggestCommand(command))
            .hoverEvent(HoverEvent.showText(c("&7Bấm để dán: &b" + command)));
    }
}
