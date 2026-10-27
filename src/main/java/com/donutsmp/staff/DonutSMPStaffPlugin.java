package com.donutsmp.staff;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DonutSMPStaffPlugin extends JavaPlugin implements Listener {

    private final Set<UUID> vanished = new HashSet<>();
    private final Set<UUID> staffMode = new HashSet<>();
    private final Set<UUID> frozen = new HashSet<>();
    private final Set<UUID> muted = new HashSet<>();
    private final Set<UUID> godMode = new HashSet<>();
    private final Map<UUID, Location> frozenLocations = new HashMap<>();
    private final Map<UUID, String> muteReasons = new HashMap<>();
    private final Map<UUID, Integer> warnings = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        getCommand("vanish").setExecutor(this::onVanishCommand);
        getCommand("sfmode").setExecutor(this::onStaffModeCommand);
        getCommand("staffchat").setExecutor(this::onStaffChatCommand);
        getCommand("freeze").setExecutor(this::onFreezeCommand);
        getCommand("mute").setExecutor(this::onMuteCommand);
        getCommand("heal").setExecutor(this::onHealCommand);
        getCommand("feed").setExecutor(this::onFeedCommand);
        getCommand("god").setExecutor(this::onGodCommand);
        getCommand("tp").setExecutor(this::onTeleportCommand);
        getCommand("tphere").setExecutor(this::onTeleportHereCommand);
        getCommand("inspect").setExecutor(this::onInspectCommand);
        getCommand("fly").setExecutor(this::onFlyCommand);
        getCommand("speed").setExecutor(this::onSpeedCommand);
        getCommand("kick").setExecutor(this::onKickCommand);
        getCommand("ban").setExecutor(this::onBanCommand);
        getCommand("warn").setExecutor(this::onWarnCommand);
        getCommand("invsee").setExecutor(this::onInventorySeeCommand);
        getCommand("report").setExecutor(this::onReportCommand);
        getCommand("clearchat").setExecutor(this::onClearChatCommand);

        getLogger().info("DonutSMPStaff enabled.");
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (vanished.contains(player.getUniqueId())) {
                updateVanishVisibility(player, false);
            }
            if (staffMode.contains(player.getUniqueId())) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                player.setAllowFlight(false);
                player.setFlying(false);
            }
            if (godMode.contains(player.getUniqueId())) {
                player.setInvulnerable(false);
            }
        }

        vanished.clear();
        staffMode.clear();
        frozen.clear();
        muted.clear();
        godMode.clear();
        frozenLocations.clear();
        muteReasons.clear();
        warnings.clear();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (vanished.remove(uuid)) {
            updateVanishVisibility(event.getPlayer(), false);
        }
        staffMode.remove(uuid);
        frozen.remove(uuid);
        frozenLocations.remove(uuid);
        muted.remove(uuid);
        muteReasons.remove(uuid);
        godMode.remove(uuid);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (vanished.contains(player.getUniqueId())) {
            updateVanishVisibility(player, true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!frozen.contains(player.getUniqueId())) {
            return;
        }
        if (event.hasChangedPosition()) {
            Location original = frozenLocations.get(player.getUniqueId());
            if (original != null) {
                player.teleport(original);
            }
            player.sendActionBar(Component.text("You are frozen by staff.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!muted.contains(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String reason = muteReasons.getOrDefault(player.getUniqueId(), "No reason provided");
        player.sendMessage(Component.text("You are muted. Reason: " + reason, NamedTextColor.RED));
    }

    private boolean onVanishCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use /vanish.");
            return true;
        }
        if (!player.hasPermission("donutsmp.staff.vanish")) {
            sender.sendMessage("You do not have permission to use /vanish.");
            return true;
        }

        boolean hidden = !vanished.contains(player.getUniqueId());
        if (hidden) {
            vanished.add(player.getUniqueId());
            updateVanishVisibility(player, true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, true));
            player.setAllowFlight(true);
            player.setFlying(true);
            player.sendMessage(Component.text("You left the server.", NamedTextColor.GRAY));
        } else {
            vanished.remove(player.getUniqueId());
            updateVanishVisibility(player, false);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.sendMessage(Component.text("You joined the server.", NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean onStaffModeCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /sfmode.");
            return true;
        }
        if (!player.hasPermission("donutsmp.staff.staffmode")) {
            sender.sendMessage("You do not have permission to use /sfmode.");
            return true;
        }

        if (staffMode.contains(player.getUniqueId())) {
            staffMode.remove(player.getUniqueId());
            if (vanished.contains(player.getUniqueId())) {
                player.performCommand("vanish");
            }
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.sendMessage(Component.text("Staff mode disabled.", NamedTextColor.YELLOW));
            return true;
        }

        staffMode.add(player.getUniqueId());
        if (!vanished.contains(player.getUniqueId())) {
            player.performCommand("vanish");
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, true));
        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage(Component.text("Staff mode enabled.", NamedTextColor.GOLD));
        return true;
    }

    private boolean onStaffChatCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /staffchat <message>");
            return true;
        }
        if (sender instanceof Player player && !player.hasPermission("donutsmp.staff.chat")) {
            sender.sendMessage("You do not have permission to use staff chat.");
            return true;
        }
        if (!(sender instanceof Player) && !sender.hasPermission("donutsmp.staff.chat")) {
            sender.sendMessage("No permission.");
            return true;
        }

        String message = String.join(" ", args);
        String formatted = "§8[§6STAFF§8] §f" + sender.getName() + "§7: §f" + message;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("donutsmp.staff.chat")) {
                online.sendMessage(formatted);
            }
        }
        return true;
    }

    private boolean onFreezeCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("Usage: /freeze <player>");
            return true;
        }
        if (!sender.hasPermission("donutsmp.staff.freeze")) {
            sender.sendMessage("You do not have permission to use /freeze.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found.");
            return true;
        }

        if (frozen.contains(target.getUniqueId())) {
            frozen.remove(target.getUniqueId());
            frozenLocations.remove(target.getUniqueId());
            target.sendMessage(Component.text("You have been unfrozen.", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("You unfroze " + target.getName() + ".", NamedTextColor.YELLOW));
            return true;
        }

        frozen.add(target.getUniqueId());
        frozenLocations.put(target.getUniqueId(), target.getLocation().clone());
        target.sendMessage(Component.text("You have been frozen by a staff member.", NamedTextColor.RED));
        sender.sendMessage(Component.text("You froze " + target.getName() + ".", NamedTextColor.YELLOW));
        return true;
    }

    private boolean onMuteCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /mute <player> [reason]");
            return true;
        }
        if (!sender.hasPermission("donutsmp.staff.mute")) {
            sender.sendMessage("You do not have permission to use /mute.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found.");
            return true;
        }

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";
        if (muted.contains(target.getUniqueId())) {
            muted.remove(target.getUniqueId());
            muteReasons.remove(target.getUniqueId());
            target.sendMessage(Component.text("You have been unmuted.", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("You unmuted " + target.getName() + ".", NamedTextColor.YELLOW));
            return true;
        }

        muted.add(target.getUniqueId());
        muteReasons.put(target.getUniqueId(), reason);
        target.sendMessage(Component.text("You have been muted. Reason: " + reason, NamedTextColor.RED));
        sender.sendMessage(Component.text("You muted " + target.getName() + ".", NamedTextColor.YELLOW));
        return true;
    }

    private boolean onHealCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutsmp.staff.heal")) {
            sender.sendMessage("You do not have permission to use /heal.");
            return true;
        }

        Player target = sender instanceof Player p ? p : null;
        if (args.length == 1) {
            target = Bukkit.getPlayerExact(args[0]);
        }
        if (target == null) {
            sender.sendMessage("Usage: /heal [player]");
            return true;
        }

        target.setHealth(target.getMaxHealth());
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.sendMessage(Component.text("You have been healed.", NamedTextColor.GREEN));
        if (sender != target) {
            sender.sendMessage(Component.text("Healed " + target.getName() + ".", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean onFeedCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutsmp.staff.feed")) {
            sender.sendMessage("You do not have permission to use /feed.");
            return true;
        }

        Player target = sender instanceof Player p ? p : null;
        if (args.length == 1) {
            target = Bukkit.getPlayerExact(args[0]);
        }
        if (target == null) {
            sender.sendMessage("Usage: /feed [player]");
            return true;
        }

        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.sendMessage(Component.text("You have been fed.", NamedTextColor.GREEN));
        if (sender != target) {
            sender.sendMessage(Component.text("Fed " + target.getName() + ".", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean onGodCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutsmp.staff.god")) {
            sender.sendMessage("You do not have permission to use /god.");
            return true;
        }

        Player target = sender instanceof Player p ? p : null;
        if (args.length == 1) {
            target = Bukkit.getPlayerExact(args[0]);
        }
        if (target == null) {
            sender.sendMessage("Usage: /god [player]");
            return true;
        }

        if (godMode.contains(target.getUniqueId())) {
            godMode.remove(target.getUniqueId());
            target.setInvulnerable(false);
            target.sendMessage(Component.text("God mode disabled.", NamedTextColor.YELLOW));
            if (sender != target) {
                sender.sendMessage(Component.text("God mode disabled for " + target.getName() + ".", NamedTextColor.YELLOW));
            }
            return true;
        }

        godMode.add(target.getUniqueId());
        target.setInvulnerable(true);
        target.sendMessage(Component.text("God mode enabled.", NamedTextColor.GOLD));
        if (sender != target) {
            sender.sendMessage(Component.text("God mode enabled for " + target.getName() + ".", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean onTeleportCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("Usage: /tp <player>");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /tp.");
            return true;
        }
        if (!player.hasPermission("donutsmp.staff.tp")) {
            sender.sendMessage("You do not have permission to use /tp.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found.");
            return true;
        }

        player.teleport(target.getLocation());
        player.sendMessage(Component.text("Teleported to " + target.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean onTeleportHereCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("Usage: /tphere <player>");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /tphere.");
            return true;
        }
        if (!player.hasPermission("donutsmp.staff.tp")) {
            sender.sendMessage("You do not have permission to use /tphere.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found.");
            return true;
        }

        target.teleport(player.getLocation());
        target.sendMessage(Component.text("You were teleported to " + player.getName() + ".", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Teleported " + target.getName() + " to you.", NamedTextColor.YELLOW));
        return true;
    }

    private boolean onInspectCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("Usage: /inspect <player>");
            return true;
        }
        if (!sender.hasPermission("donutsmp.staff.inspect")) {
            sender.sendMessage("You do not have permission to use /inspect.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found.");
            return true;
        }

        sender.sendMessage(Component.text("=== Player Check ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Name: " + target.getName(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("UUID: " + target.getUniqueId(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("GameMode: " + target.getGameMode(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Health: " + target.getHealth() + "/" + target.getMaxHealth(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Food: " + target.getFoodLevel(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Location: " + target.getLocation().getWorld().getName() + " " + target.getLocation().getBlockX() + ", " + target.getLocation().getBlockY() + ", " + target.getLocation().getBlockZ(), NamedTextColor.YELLOW));
        return true;
    }

    private boolean onFlyCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutsmp.staff.fly")) {
            sender.sendMessage("You do not have permission to use /fly.");
            return true;
        }

        Player target = sender instanceof Player p ? p : null;
        if (args.length == 1) {
            target = Bukkit.getPlayerExact(args[0]);
        }
        if (target == null) {
            sender.sendMessage("Usage: /fly [player]");
            return true;
        }

        target.setAllowFlight(!target.getAllowFlight());
        target.setFlying(target.getAllowFlight());
        target.sendMessage(Component.text("Flight toggled to " + target.getAllowFlight() + ".", NamedTextColor.GREEN));
        if (sender != target) {
            sender.sendMessage(Component.text("Flight toggled for " + target.getName() + ".", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean onSpeedCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutsmp.staff.speed")) {
            sender.sendMessage("You do not have permission to use /speed.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("Usage: /speed [player] [value]");
            return true;
        }

        Player target = sender instanceof Player p ? p : null;
        float speed = 1.0F;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[0]);
            try {
                speed = Float.parseFloat(args[1]);
            } catch (NumberFormatException ignored) {
                sender.sendMessage("Speed must be a number.");
                return true;
            }
        } else {
            try {
                speed = Float.parseFloat(args[0]);
            } catch (NumberFormatException ignored) {
                sender.sendMessage("Speed must be a number.");
                return true;
            }
        }

        if (target == null) {
            sender.sendMessage("Player not found.");
            return true;
        }

        target.setWalkSpeed(speed / 10.0F);
        target.setFlySpeed(speed / 10.0F);
        sender.sendMessage(Component.text("Set speed for " + target.getName() + " to " + speed + ".", NamedTextColor.YELLOW));
        return true;
    }

    private boolean onKickCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /kick <player> [reason]");
            return true;
        }
        if (!sender.hasPermission("donutsmp.staff.kick")) {
            sender.sendMessage("You do not have permission to use /kick.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found.");
            return true;
        }

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Kicked by staff";
        target.kick(Component.text("You were kicked. Reason: " + reason, NamedTextColor.RED));
        sender.sendMessage(Component.text("Kicked " + target.getName() + ".", NamedTextColor.YELLOW));
        return true;
    }

    private boolean onBanCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /ban <player> [reason]");
            return true;
        }
        if (!sender.hasPermission("donutsmp.staff.ban")) {
            sender.sendMessage("You do not have permission to use /ban.");
            return true;
        }

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Banned by staff";
        String targetName = args[0];
        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(targetName, reason, null, sender.getName());
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            target.kick(Component.text("You have been banned. Reason: " + reason, NamedTextColor.RED));
        }
        sender.sendMessage(Component.text("Banned " + targetName + ".", NamedTextColor.YELLOW));
        return true;
    }

    private boolean onWarnCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /warn <player> <reason>");
            return true;
        }
        if (!sender.hasPermission("donutsmp.staff.warn")) {
            sender.sendMessage("You do not have permission to use /warn.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found.");
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        int count = warnings.getOrDefault(target.getUniqueId(), 0) + 1;
        warnings.put(target.getUniqueId(), count);
        target.sendMessage(Component.text("You were warned. Warning #" + count + ": " + reason, NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Warned " + target.getName() + ".", NamedTextColor.YELLOW));
        return true;
    }

    private boolean onInventorySeeCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("Usage: /invsee <player>");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /invsee.");
            return true;
        }
        if (!player.hasPermission("donutsmp.staff.invsee")) {
            sender.sendMessage("You do not have permission to use /invsee.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found.");
            return true;
        }

        Inventory inventory = Bukkit.createInventory(null, 54, target.getName() + "'s Inventory");
        inventory.setContents(target.getInventory().getContents());
        player.openInventory(inventory);
        return true;
    }

    private boolean onReportCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /report <player> <reason>");
            return true;
        }
        if (!sender.hasPermission("donutsmp.staff.report")) {
            sender.sendMessage("You do not have permission to use /report.");
            return true;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Bukkit.getLogger().info("Staff report: " + sender.getName() + " reported " + targetName + " for: " + reason);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("donutsmp.staff.chat")) {
                online.sendMessage(Component.text("[REPORT] " + sender.getName() + " reported " + targetName + " for: " + reason, NamedTextColor.RED));
            }
        }
        sender.sendMessage(Component.text("Report submitted for " + targetName + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean onClearChatCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donutsmp.staff.clearchat")) {
            sender.sendMessage("You do not have permission to use /clearchat.");
            return true;
        }

        for (int i = 0; i < 100; i++) {
            Bukkit.broadcastMessage(" ");
        }
        Bukkit.broadcast(Component.text("Chat cleared by " + sender.getName() + ".", NamedTextColor.GOLD));
        return true;
    }

    private void updateVanishVisibility(Player player, boolean hidden) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) {
                continue;
            }
            if (hidden) {
                if (!other.hasPermission("donutsmp.staff.seevanish")) {
                    other.hidePlayer(this, player);
                }
            } else {
                other.showPlayer(this, player);
            }
        }
    }
}
