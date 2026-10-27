package com.donutsmp.staff;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class StaffMenu implements Listener {

    private static final String MENU_TITLE = "VerdantSMP Staff";

    public StaffMenu(DonutSMPStaffPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public Inventory createMainInventory() {
        Inventory inv = Bukkit.createInventory(null, 9 * 6, MENU_TITLE);
        fillBackground(inv);

        inv.setItem(10, createItem(Material.ENDER_EYE, "Vanish", "Toggle vanish mode"));
        inv.setItem(11, createItem(Material.DIAMOND_SWORD, "Staff Mode", "Toggle staff mode"));
        inv.setItem(12, createItem(Material.OAK_SIGN, "Staff Chat", "Chat with staff only"));

        inv.setItem(19, createItem(Material.ICE, "Freeze", "Freeze or unfreeze a player"));
        inv.setItem(20, createItem(Material.BOOK, "Mute", "Mute or unmute a player"));
        inv.setItem(21, createItem(Material.GOLDEN_APPLE, "Heal", "Restore health and hunger"));
        inv.setItem(22, createItem(Material.COOKED_BEEF, "Feed", "Fill hunger"));
        inv.setItem(23, createItem(Material.SHIELD, "God Mode", "Toggle invulnerability"));

        inv.setItem(28, createItem(Material.ENDER_PEARL, "TP", "Teleport to a player"));
        inv.setItem(29, createItem(Material.ENDER_PEARL, "TP Here", "Bring a player to you"));
        inv.setItem(30, createItem(Material.COMPASS, "Inspect", "Inspect player stats"));
        inv.setItem(31, createItem(Material.FEATHER, "Fly", "Toggle flight"));
        inv.setItem(32, createItem(Material.SUGAR, "Speed", "Set speed"));

        inv.setItem(37, createItem(Material.BARRIER, "Kick", "Kick a player"));
        inv.setItem(38, createItem(Material.REDSTONE_BLOCK, "Ban", "Ban a player"));
        inv.setItem(39, createItem(Material.PAPER, "Warn", "Warn a player"));
        inv.setItem(40, createItem(Material.CHEST, "Invsee", "Open inventory"));
        inv.setItem(41, createItem(Material.WRITABLE_BOOK, "Report", "Report a player"));

        inv.setItem(49, createItem(Material.STRUCTURE_VOID, "Clear Chat", "Clear server chat"));
        inv.setItem(53, createItem(Material.BEACON, "Close", "Close the menu"));

        return inv;
    }

    private void fillBackground(Inventory inv) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        paneMeta.displayName(Component.text(" ", NamedTextColor.BLACK));
        pane.setItemMeta(paneMeta);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }

        ItemStack header = createItem(Material.EMERALD, "VerdantSMP", "Staff tools");
        inv.setItem(4, header);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!MENU_TITLE.equals(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!player.hasPermission("donutsmp.staff.menu")) {
            player.sendMessage(Component.text("You do not have access to the VerdantSMP Staff menu.", NamedTextColor.RED));
            return;
        }

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) {
            return;
        }

        if (current.getType() == Material.BEACON && current.hasItemMeta() && current.getItemMeta().hasDisplayName()
                && "Close".equals(current.getItemMeta().getDisplayName())) {
            player.closeInventory();
            return;
        }

        String name = current.hasItemMeta() && current.getItemMeta().hasDisplayName()
                ? current.getItemMeta().getDisplayName()
                : "";

        switch (name) {
            case "Vanish" -> player.performCommand("vanish");
            case "Staff Mode" -> player.performCommand("sfmode");
            case "Staff Chat" -> player.sendMessage(Component.text("Use /staffchat <message>", NamedTextColor.YELLOW));
            case "Freeze" -> player.sendMessage(Component.text("Use /freeze <player>", NamedTextColor.YELLOW));
            case "Mute" -> player.sendMessage(Component.text("Use /mute <player> [reason]", NamedTextColor.YELLOW));
            case "Heal" -> player.performCommand("heal");
            case "Feed" -> player.performCommand("feed");
            case "God Mode" -> player.performCommand("god");
            case "TP" -> player.sendMessage(Component.text("Use /tp <player>", NamedTextColor.YELLOW));
            case "TP Here" -> player.sendMessage(Component.text("Use /tphere <player>", NamedTextColor.YELLOW));
            case "Inspect" -> player.sendMessage(Component.text("Use /inspect <player>", NamedTextColor.YELLOW));
            case "Fly" -> player.performCommand("fly");
            case "Speed" -> player.sendMessage(Component.text("Use /speed <player> [value]", NamedTextColor.YELLOW));
            case "Ban" -> player.sendMessage(Component.text("Use /ban <player> [reason]", NamedTextColor.YELLOW));
            case "Kick" -> player.sendMessage(Component.text("Use /kick <player> [reason]", NamedTextColor.YELLOW));
            case "Warn" -> player.sendMessage(Component.text("Use /warn <player> <reason>", NamedTextColor.YELLOW));
            case "Invsee" -> player.sendMessage(Component.text("Use /invsee <player>", NamedTextColor.YELLOW));
            case "Report" -> player.sendMessage(Component.text("Use /report <player> <reason>", NamedTextColor.YELLOW));
            case "Clear Chat" -> player.performCommand("clearchat");
            default -> {
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (MENU_TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    private ItemStack createItem(Material material, String name, String loreText) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(loreText, NamedTextColor.GRAY));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }
}
